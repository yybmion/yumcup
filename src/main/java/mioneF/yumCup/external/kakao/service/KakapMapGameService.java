package mioneF.yumCup.external.kakao.service;

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
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.service.GameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class KakapMapGameService {

    private final KakaoMapRestaurantService kakaoMapService;
    private final GameService gameService;
    private final GameRepository gameRepository;

    public KakapMapGameService(KakaoMapRestaurantService kakaoMapService,
                               GameService gameService,
                               GameRepository gameRepository) {
        this.kakaoMapService = kakaoMapService;
        this.gameService = gameService;
        this.gameRepository = gameRepository;
    }

    public MatchResult selectWinner(Long gameId, Long matchId, Long winnerId) {
        return gameService.selectWinner(gameId, matchId, winnerId);
    }

    public GameResponse startLocationBasedGame(LocationRequest request) {

        // 1) API 호출 부분 분리 - 트랜잭션 없이 실행
        List<Restaurant> restaurants = searchAndPrepareRestaurants(
                request.latitude(),
                request.longitude(),
                request.radius()
        );

        // 2) DB 작업만 트랜잭션으로 처리
        return createGameWithRestaurants(restaurants);
    }

    public List<Restaurant> searchAndPrepareRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> kakaoRestaurants = kakaoMapService.searchNearbyRestaurants(latitude, longitude, radius);
        return kakaoRestaurants;
    }

    // DB 작업 부분 - 트랜잭션 처리
    @Transactional
    public GameResponse createGameWithRestaurants(List<Restaurant> restaurants) {
        // 이미 저장된 Restaurant들을 사용하므로 추가 저장 없이 진행
        List<Restaurant> selectedRestaurants = restaurants.stream()
                .limit(16)
                .collect(Collectors.toList());

        Game game = Game.builder()
                .totalRounds(16)
                .build();

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
    }
}
