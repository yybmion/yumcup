package mioneF.yumCup.external.kakao.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.Game;
import mioneF.yumCup.domain.entity.Match;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import mioneF.yumCup.service.GameService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class KakapMapGameService {

	private final KakaoMapRestaurantService kakaoMapService;
	private final GameService gameService;
	private final GameRepository gameRepository;
	private final RestaurantRepository restaurantRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final int GEOHASH_PRECISION = 6;

	public KakapMapGameService(
			KakaoMapRestaurantService kakaoMapService,
			GameService gameService,
			GameRepository gameRepository,
			RestaurantRepository restaurantRepository,
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper) {
		this.kakaoMapService = kakaoMapService;
		this.gameService = gameService;
		this.gameRepository = gameRepository;
		this.restaurantRepository = restaurantRepository;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
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

	public List<Restaurant> searchAndPrepareRestaurants(
			Double latitude, Double longitude, Integer radius) {

		String geohash = generateGeohash( latitude, longitude, GEOHASH_PRECISION );
		String cacheKey = "restaurants:kakaoIds:geohash:" + geohash + ":" + radius;

		log.info( "Cache key: {}", cacheKey );

		try {
			String cached = redisTemplate.opsForValue().get( cacheKey );
			if ( cached != null ) {
				List<String> kakaoIds = objectMapper.readValue(
						cached, new TypeReference<List<String>>() {
						}
				);
				List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn( kakaoIds );

				if ( restaurants.size() == kakaoIds.size() ) {
					Map<String, Restaurant> map = restaurants.stream()
							.collect( Collectors.toMap( Restaurant::getKakaoId, r -> r ) );
					return kakaoIds.stream().map( map::get ).collect( Collectors.toList() );
				}
				redisTemplate.delete( cacheKey );
			}
		}
		catch (Exception e) {
			log.warn( "Redis read failed: {}", e.getMessage() );
		}

		List<Restaurant> restaurants = kakaoMapService.searchNearbyRestaurants( latitude, longitude, radius );

		try {
			List<String> kakaoIds = restaurants.stream()
					.map( Restaurant::getKakaoId )
					.collect( Collectors.toList() );

			String json = objectMapper.writeValueAsString( kakaoIds );

			redisTemplate.opsForValue().set( cacheKey, json, 1, TimeUnit.HOURS );

			String verify = redisTemplate.opsForValue().get( cacheKey );

		}
		catch (Exception e) {
			log.error( "Redis write failed: ", e );
		}

		return restaurants;
	}

	private String generateGeohash(Double latitude, Double longitude, int precision) {
		return GeoHash.withCharacterPrecision( latitude, longitude, precision ).toBase32();
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
