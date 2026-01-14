package mioneF.yumCup.external.kakao.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.exception.RestaurantProcessingException;
import mioneF.yumCup.exception.RestaurantProcessingTimeoutException;
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
	private static final int TIMEOUT_SECONDS = 5;
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
				.collect( Collectors.toList() );

		cacheStrategy.put( cacheKey, kakaoIds, CACHE_TTL_SECONDS );
		log.info( "Cached {} restaurant IDs", kakaoIds.size() );

		Collections.shuffle( savedRestaurants );
		return savedRestaurants;
	}

	/**
	 * Kakao API로 레스토랑 수집
	 */
	private List<Restaurant> fetchRestaurantsFromKakao(Double latitude, Double longitude, Integer radius) {
		List<Restaurant> allRestaurants = new ArrayList<>();
		int page = 1;

		while ( allRestaurants.size() < REQUIRED_RESTAURANTS ) {
			KakaoSearchResponse response = fetchRestaurantsPage( latitude, longitude, radius, page );

			if ( response == null || response.documents().isEmpty() ) {
				throw new NoNearbyRestaurantsException( "Can't found any around restaurant" );
			}

			int remaining = REQUIRED_RESTAURANTS - allRestaurants.size();
			int toProcess = Math.min( remaining, response.documents().size() );

			log.info(
					"Processing {} restaurants in parallel (page {}, need {} more)",
					toProcess, page, remaining
			);

			List<CompletableFuture<Restaurant>> futures = response.documents().stream()
					.limit( toProcess )
					.map( doc -> CompletableFuture.supplyAsync(
							() -> {
								try {
									return enrichmentService.enrichWithGoogleInfo( doc );
								}
								catch (Exception e) {
									log.error(
											"Error processing restaurant {}: {}",
											doc.place_name(), e.getMessage()
									);
									throw new RestaurantProcessingException( "Error processing restaurant" );
								}
							},
							executorService
					) )
					.collect( Collectors.toList() );

			List<Restaurant> pageRestaurants = collectRestaurantResults( futures );
			log.info(
					"Completed processing {} restaurants ({} total)",
					pageRestaurants.size(), allRestaurants.size() + pageRestaurants.size()
			);

			allRestaurants.addAll( pageRestaurants );

			if ( allRestaurants.size() >= REQUIRED_RESTAURANTS ) {
				break;
			}

			if ( response.meta().is_end() ) {
				break;
			}
			page++;
		}

		if ( allRestaurants.size() < REQUIRED_RESTAURANTS ) {
			throw new InsufficientRestaurantsException(
					String.format( "Need at least 16 restaurants, but found only %d", allRestaurants.size() )
			);
		}

		return allRestaurants;
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

	/**
	 * 병렬 처리 결과 수집
	 */
	private List<Restaurant> collectRestaurantResults(List<CompletableFuture<Restaurant>> futures) {
		return futures.stream()
				.map( this::getRestaurantWithTimeout )
				.collect( Collectors.toList() );
	}

	/**
	 * 타임아웃과 함께 Future 결과 가져오기
	 */
	private Restaurant getRestaurantWithTimeout(CompletableFuture<Restaurant> future) {
		try {
			return future.get( TIMEOUT_SECONDS, TimeUnit.SECONDS );
		}
		catch (TimeoutException e) {
			throw new RestaurantProcessingTimeoutException( "Restaurant information processing timeout" );
		}
		catch (Exception e) {
			throw new RestaurantProcessingException( "Error processing restaurant information" );
		}
	}
}
