package mioneF.yumCup.external.kakao.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.Game;
import mioneF.yumCup.domain.entity.Match;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.infrastructure.cache.GeohashCacheStrategy;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import mioneF.yumCup.service.GameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class KakapMapGameService {

	private final KakaoMapRestaurantService kakaoMapService;
	private final GameService gameService;
	private final GameRepository gameRepository;
	private final RestaurantRepository restaurantRepository;
	private final GeohashCacheStrategy cacheStrategy;

	private static final long CACHE_TTL_SECONDS = 3600;

	public KakapMapGameService(
			KakaoMapRestaurantService kakaoMapService,
			GameService gameService,
			GameRepository gameRepository,
			RestaurantRepository restaurantRepository,
			GeohashCacheStrategy cacheStrategy) {
		this.kakaoMapService = kakaoMapService;
		this.gameService = gameService;
		this.gameRepository = gameRepository;
		this.restaurantRepository = restaurantRepository;
		this.cacheStrategy = cacheStrategy;
	}

	public MatchResult selectWinner(Long gameId, Long matchId, Long winnerId) {
		return gameService.selectWinner( gameId, matchId, winnerId );
	}

	public GameResponse startLocationBasedGame(LocationRequest request) {
		List<Restaurant> restaurants = searchAndPrepareRestaurants(
				request.latitude(),
				request.longitude(),
				request.radius()
		);
		return createGameWithRestaurants( restaurants );
	}

	/**
	 * 레스토랑 검색 및 준비 (캐싱 전략 통일)
	 * GeohashCacheStrategy를 사용하여 일관된 캐싱 처리
	 */
	public List<Restaurant> searchAndPrepareRestaurants(
			Double latitude, Double longitude, Integer radius) {

		// GeohashCacheStrategy를 사용하여 캐시 키 생성 (일관된 방식)
		String cacheKey = cacheStrategy.generateGeohashKey(
				"restaurants:kakaoIds",
				latitude,
				longitude,
				String.valueOf( radius )
		);

		log.info( "Cache key: {}", cacheKey );

		// 캐시 조회 - GeohashCacheStrategy 사용
		Optional<List> cachedList = cacheStrategy.get( cacheKey, List.class );
		if ( cachedList.isPresent() ) {
			@SuppressWarnings("unchecked")
			List<String> kakaoIds = (List<String>) cachedList.get();
			List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn( kakaoIds );

			if ( restaurants.size() == kakaoIds.size() ) {
				log.info( "Cache HIT: Returning {} restaurants", restaurants.size() );
				// LinkedHashMap으로 순서 보존하며 단일 패스 처리
				Map<String, Restaurant> map = restaurants.stream()
						.collect( Collectors.toMap(
								Restaurant::getKakaoId,
								r -> r,
								(a, b) -> a,
								LinkedHashMap::new
						) );
				return kakaoIds.stream().map( map::get ).toList();
			}
			// 캐시 데이터 불일치 시 삭제
			log.warn( "Cache data mismatch, evicting cache" );
			cacheStrategy.evict( cacheKey );
		}

		log.info( "Cache MISS: Fetching restaurants from API" );
		List<Restaurant> restaurants = kakaoMapService.searchNearbyRestaurants( latitude, longitude, radius );

		// 캐시 저장 - GeohashCacheStrategy 사용
		List<String> kakaoIds = restaurants.stream()
				.map( Restaurant::getKakaoId )
				.toList();

		cacheStrategy.put( cacheKey, kakaoIds, CACHE_TTL_SECONDS );
		log.info( "Cached {} restaurant IDs", kakaoIds.size() );

		return restaurants;
	}

	@Transactional
	public GameResponse createGameWithRestaurants(List<Restaurant> restaurants) {
		List<Restaurant> selectedRestaurants = restaurants.stream()
				.limit( 16 )
				.collect( Collectors.toList() );

		Game game = Game.builder().totalRounds( 16 ).build();

		for ( int i = 0; i < selectedRestaurants.size(); i += 2 ) {
			Match match = Match.builder()
					.restaurant1( selectedRestaurants.get( i ) )
					.restaurant2( selectedRestaurants.get( i + 1 ) )
					.round( 16 )
					.matchOrder( ( i / 2 ) + 1 )
					.build();
			game.addMatch( match );
		}

		Game savedGame = gameRepository.save( game );
		Match firstMatch = savedGame.getMatches().get( 0 );

		return new GameResponse(
				savedGame.getId(),
				16,
				new MatchResponse(
						firstMatch.getId(),
						RestaurantResponse.from( firstMatch.getRestaurant1() ),
						RestaurantResponse.from( firstMatch.getRestaurant2() ),
						firstMatch.getRound(),
						firstMatch.getMatchOrder()
				),
				savedGame.getStatus()
		);
	}
}
