package mioneF.yumCup.external.kakao.service;

import java.util.Collections;
import java.util.List;
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
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import mioneF.yumCup.service.GameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class KakapMapGameService {
    private final KakaoMapRestaurantService kakaoMapService;
    private final GameService gameService;
    private final RestaurantRepository restaurantRepository;
    private final GameRepository gameRepository;

    public KakapMapGameService(
            KakaoMapRestaurantService kakaoMapService,
            RestaurantRepository restaurantRepository,
            GameRepository gameRepository,
            GameService gameService) {
        this.kakaoMapService = kakaoMapService;
        this.restaurantRepository = restaurantRepository;
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        log.info("KakapMapGameService initialized with kakaoMapService: {}",
                kakaoMapService != null ? "injected" : "null");
    }

    public MatchResult selectWinner(Long gameId, Long matchId, Long winnerId) {
        return gameService.selectWinner(gameId, matchId, winnerId);
    }

    @Transactional
    public GameResponse startLocationBasedGame(LocationRequest request) {
        try {
            log.info("Starting location-based game with request: {}", request);

            List<Restaurant> restaurants = searchAndPrepareRestaurants(
                    request.latitude(),
                    request.longitude(),
                    request.radius()
            );

            log.info("Found {} restaurants", restaurants.size());

            if (restaurants.isEmpty()) {
                log.warn("No restaurants found in the area");
                throw new InsufficientRestaurantsException("주변 음식점을 찾을 수 없습니다.");
            }

            if (restaurants.size() < 16) {
                log.warn("Not enough restaurants: {}", restaurants.size());
                throw new InsufficientRestaurantsException(
                        "Need at least 16 restaurants, but found: " + restaurants.size());
            }

            // 2. 무작위로 16개 선택
            Collections.shuffle(restaurants);
            List<Restaurant> selectedRestaurants = restaurants.stream()
                    .limit(16)
                    .collect(Collectors.toList());

            // 3. 게임 생성
            Game game = Game.builder()
                    .totalRounds(16)
                    .build();

            // 4. 매치 생성
            for (int i = 0; i < selectedRestaurants.size(); i += 2) {
                Match match = Match.builder()
                        .restaurant1(selectedRestaurants.get(i))
                        .restaurant2(selectedRestaurants.get(i + 1))
                        .round(16)
                        .matchOrder((i / 2) + 1)
                        .build();

                game.addMatch(match);
            }

            Game savedGame = gameRepository.save(game);
            Match firstMatch = savedGame.getMatches().get(0);

            return new GameResponse(
                    savedGame.getId(),
                    16,
                    new MatchResponse(
                            firstMatch.getId(),
                            RestaurantResponse.from(firstMatch.getRestaurant1()),
                            RestaurantResponse.from(firstMatch.getRestaurant2()),
                            firstMatch.getRound(),
                            firstMatch.getMatchOrder()
                    ),
                    savedGame.getStatus()
            );
        } catch (Exception e) {
            log.error("Error in startLocationBasedGame: ", e);
            throw new RuntimeException("주변 음식점을 찾을 수 없습니다: " + e.getMessage());
        }
    }

    @Transactional
    public List<Restaurant> searchAndPrepareRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> kakaoRestaurants = kakaoMapService.searchNearbyRestaurants(latitude, longitude, radius);

        return kakaoRestaurants.stream()
                .map(restaurant -> {
                    List<Restaurant> existingRestaurants = restaurantRepository.findByKakaoId(restaurant.getKakaoId());
                    if (!existingRestaurants.isEmpty()) {
                        Restaurant existing = existingRestaurants.get(0);
                        existing.updateWithNewInfo(restaurant);  // 모든 정보 업데이트
                        return restaurantRepository.save(existing);  // 변경사항 저장
                    }
                    return restaurantRepository.save(restaurant);
                })
                .collect(Collectors.toList());
    }
}
