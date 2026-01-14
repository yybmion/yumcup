package mioneF.yumCup.external.kakao.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import mioneF.yumCup.external.kakao.dto.KakaoSearchResponse;
import mioneF.yumCup.infrastructure.api.KakaoLocalApiClient;
import mioneF.yumCup.infrastructure.cache.GeohashCacheStrategy;
import mioneF.yumCup.performance.Monitored;
import org.springframework.stereotype.Service;

/**
 * 레스토랑 검색 조율 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapRestaurantService {

	private static final int REQUIRED_RESTAURANTS = 16;
	private static final int KAKAO_PAGE_SIZE = 15;
	private static final long CACHE_TTL_SECONDS = 3600;

	private final KakaoLocalApiClient kakaoApiClient;
	private final RestaurantEnrichmentService enrichmentService;
	private final RestaurantPersistenceService persistenceService;
	private final GeohashCacheStrategy cacheStrategy;

	private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

	@PreDestroy
	public void shutdown() {
		executorService.shutdown();
		try {
			if ( !executorService.awaitTermination( 10, TimeUnit.SECONDS ) ) {
				executorService.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * 주변 레스토랑 검색 (조율 메서드)
	 */
	@Monitored
	public List<Restaurant> searchNearbyRestaurants(Double latitude, Double longitude, Integer radius) {
		log.info( "Searching restaurants: location=({}, {}), radius={}m", latitude, longitude, radius );

		String cacheKey = cacheStrategy.generateGeohashKey(
				"restaurants:kakaoIds",
				latitude,
				longitude,
				String.valueOf( radius )
		);

		Optional<List> cachedKakaoIds = cacheStrategy.get( cacheKey, List.class );

		if ( cachedKakaoIds.isPresent() ) {
			@SuppressWarnings("unchecked")
			List<String> kakaoIds = (List<String>) cachedKakaoIds.get();
			log.info( "Cache HIT: Returning {} restaurants from cache", kakaoIds.size() );

			List<Restaurant> restaurants = persistenceService.findByKakaoIds( kakaoIds );
			Collections.shuffle( restaurants );

			return restaurants;
		}

		log.info( "Cache MISS: Fetching restaurants from APIs" );

		List<Restaurant> allRestaurants = fetchRestaurantsFromKakao( latitude, longitude, radius );

		List<Restaurant> savedRestaurants = persistenceService.saveOrUpdate( allRestaurants );

		List<String> kakaoIds = savedRestaurants.stream()
				.map( Restaurant::getKakaoId )
				.toList();

		cacheStrategy.put( cacheKey, kakaoIds, CACHE_TTL_SECONDS );
		log.info( "Cached {} restaurant IDs", kakaoIds.size() );

		Collections.shuffle( savedRestaurants );
		return savedRestaurants;
	}

	/**
	 * Kakao API로 레스토랑 수집 (병렬 페이징 + 일괄 비동기 처리)
	 * 메모리 최적화: 중간 List 대신 배열 사용, allOf().join()으로 일괄 대기
	 */
	private List<Restaurant> fetchRestaurantsFromKakao(Double latitude, Double longitude, Integer radius) {
		int pagesNeeded = (int) Math.ceil( (double) REQUIRED_RESTAURANTS / KAKAO_PAGE_SIZE );

		log.info( "Fetching {} pages from Kakao API in parallel", pagesNeeded );

		// 1. 카카오 API 병렬 호출 - 배열로 직접 생성하여 중간 리스트 제거
		@SuppressWarnings("unchecked")
		CompletableFuture<KakaoSearchResponse>[] kakaoFutures = IntStream.rangeClosed( 1, pagesNeeded )
				.mapToObj( page -> CompletableFuture.supplyAsync(
						() -> fetchRestaurantsPage( latitude, longitude, radius, page ),
						executorService
				) )
				.toArray( CompletableFuture[]::new );

		// 2. 모든 카카오 API 호출 완료 대기 후 결과 수집
		CompletableFuture.allOf( kakaoFutures ).join();

		List<KakaoDocument> allDocuments = Arrays.stream( kakaoFutures )
				.map( this::getCompletedKakaoResponse )
				.filter( Objects::nonNull )
				.flatMap( response -> response.documents().stream() )
				.limit( REQUIRED_RESTAURANTS )
				.toList();

		if ( allDocuments.isEmpty() ) {
			throw new NoNearbyRestaurantsException( "Can't find any nearby restaurant" );
		}

		log.info(
				"Collected {} documents from Kakao, starting Google API enrichment in parallel",
				allDocuments.size()
		);

		// 3. 구글 API 일괄 병렬 호출 - 배열로 직접 생성
		@SuppressWarnings("unchecked")
		CompletableFuture<Restaurant>[] googleFutures = allDocuments.stream()
				.map( doc -> CompletableFuture.supplyAsync(
						() -> enrichWithGoogleInfoSafe( doc ),
						executorService
				) )
				.toArray( CompletableFuture[]::new );

		// 4. 모든 구글 API 호출 완료 대기 후 결과 수집
		CompletableFuture.allOf( googleFutures ).join();

		List<Restaurant> allRestaurants = Arrays.stream( googleFutures )
				.map( this::getCompletedRestaurantResult )
				.filter( Objects::nonNull )
				.toList();

		log.info( "Completed processing {} restaurants", allRestaurants.size() );

		if ( allRestaurants.size() < REQUIRED_RESTAURANTS ) {
			throw new InsufficientRestaurantsException(
					String.format( "Need at least 16 restaurants, but found only %d", allRestaurants.size() )
			);
		}
		return allRestaurants;
	}

	/**
	 * 완료된 Kakao Future에서 결과 추출 (allOf 이후 호출)
	 */
	private KakaoSearchResponse getCompletedKakaoResponse(CompletableFuture<KakaoSearchResponse> future) {
		try {
			return future.getNow( null );
		}
		catch (Exception e) {
			log.error( "Error fetching from Kakao API: {}", e.getMessage() );
			return null;
		}
	}

	/**
	 * Google API enrichment을 안전하게 처리
	 */
	private Restaurant enrichWithGoogleInfoSafe(KakaoDocument doc) {
		try {
			return enrichmentService.enrichWithGoogleInfo( doc );
		}
		catch (Exception e) {
			log.error( "Error processing restaurant {}: {}", doc.place_name(), e.getMessage() );
			return null;
		}
	}

	/**
	 * 완료된 Restaurant Future에서 결과 추출 (allOf 이후 호출)
	 */
	private Restaurant getCompletedRestaurantResult(CompletableFuture<Restaurant> future) {
		try {
			return future.getNow( null );
		}
		catch (Exception e) {
			log.error( "Error getting restaurant result: {}", e.getMessage() );
			return null;
		}
	}

	/**
	 * Kakao API 페이지 조회
	 */
	private KakaoSearchResponse fetchRestaurantsPage(
			Double latitude,
			Double longitude,
			Integer radius,
			int page) {

		return kakaoApiClient.searchByCategory(
				latitude,
				longitude,
				radius,
				page,
				KakaoSearchResponse.class
		);
	}
}
