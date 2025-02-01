package mioneF.yumCup.GameTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.Game;
import mioneF.yumCup.domain.entity.GameStatus;
import mioneF.yumCup.domain.entity.Match;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.external.kakao.service.KakaoMapRestaurantService;
import mioneF.yumCup.external.kakao.service.KakapMapGameService;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GameStartUnitTest {
    @Mock
    private KakaoMapRestaurantService kakaoMapService;

    @Mock
    private GameService gameService;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private KakapMapGameService kakapMapGameService;

    @Nested
    @DisplayName("게임 시작 - startLocationBasedGame")
    class StartGame {
        @Test
        @DisplayName("위치 정보를 받아 근처 16개의 식당으로 토너먼트를 시작한다")
        void 성공_토너먼트생성_유효한파라미터() {
            // given
            // 사용자의 위치 정보로 게임 시작 요청
            LocationRequest request = new LocationRequest(37.5, 127.0, 500);
            List<Restaurant> restaurants = createTestRestaurants(16);
            Game game = createTestGame(restaurants);

            // 외부 API 호출로 주변 식당 정보를 가져옴
            when(kakaoMapService.searchNearbyRestaurants(37.5, 127.0, 500))
                    .thenReturn(restaurants);
            when(gameRepository.save(any(Game.class))).thenReturn(game);

            // when
            GameResponse response = kakapMapGameService.startLocationBasedGame(request);

            // then
            assertThat(response.currentRound()).isEqualTo(16);
            assertThat(response.currentMatch().matchOrder()).isEqualTo(1);
            assertThat(response.status()).isEqualTo(GameStatus.PROGRESS);
        }
    }

    @Nested
    @DisplayName("승자 선택 - selectWinner")
    class SelectWinner {
        @Test
        @DisplayName("매치의 승자를 선택하면 다음 라운드가 시작된다")
        void 성공_승자선택_유효한파라미터() {
            // given
            RestaurantResponse winner = RestaurantResponse.from(
                    Restaurant.builder()
                            .name("맛있는 식당")
                            .category("한식")
                            .build()
            );

            // 다음 매치 정보 준비
            MatchResponse nextMatch = new MatchResponse(
                    2L,  // 다음 매치 ID
                    RestaurantResponse.from(createTestRestaurant(2L)),  // 다음 매치의 레스토랑1
                    RestaurantResponse.from(createTestRestaurant(3L)),  // 다음 매치의 레스토랑2
                    8,   // 8강
                    1    // 매치 순서
            );

            MatchResult expectedResult = new MatchResult(false, nextMatch, winner);

            when(gameService.selectWinner(1L, 1L, winner.id()))
                    .thenReturn(expectedResult);

            // when
            MatchResult result = kakapMapGameService.selectWinner(1L, 1L, winner.id());

            // then
            assertThat(result.gameComplete()).isFalse();  // 게임이 아직 진행 중
            assertThat(result.nextMatch()).isNotNull();   // 다음 매치 정보가 있어야 함
            assertThat(result.winner()).isEqualTo(winner); // 승자 정보가 정확해야 함
        }

        @Test
        @DisplayName("결승전의 승자를 선택하면 게임이 종료된다")
        void 성공_게임종료_결승전승자선택() {
            // given
            RestaurantResponse winner = RestaurantResponse.from(
                    Restaurant.builder()
                            .name("우승 식당")
                            .category("한식")
                            .build()
            );

            // 결승전이므로 다음 매치는 없고(null), gameComplete는 true
            MatchResult expectedResult = new MatchResult(true, null, winner);

            when(gameService.selectWinner(1L, 15L, winner.id()))
                    .thenReturn(expectedResult);

            // when
            MatchResult result = kakapMapGameService.selectWinner(1L, 15L, winner.id());

            // then
            assertThat(result.gameComplete()).isTrue();    // 게임이 완료됨
            assertThat(result.nextMatch()).isNull();       // 다음 매치가 없어야 함
            assertThat(result.winner()).isEqualTo(winner); // 최종 우승자 정보가 정확해야 함
        }
    }

    @Nested
    @DisplayName("식당 검색 - searchAndPrepareRestaurants")
    class SearchRestaurants {
        @Test
        @DisplayName("위치 기반으로 주변 식당을 검색한다")
        void 성공_식당검색_유효한파라미터() {
            // given
            double latitude = 37.5;
            double longitude = 127.0;
            int radius = 500;  // 500m 반경 내 검색
            List<Restaurant> expectedRestaurants = createTestRestaurants(16);

            when(kakaoMapService.searchNearbyRestaurants(latitude, longitude, radius))
                    .thenReturn(expectedRestaurants);

            // when
            List<Restaurant> restaurants = kakapMapGameService
                    .searchAndPrepareRestaurants(latitude, longitude, radius);

            // then
            assertThat(restaurants).hasSize(16);
            assertThat(restaurants).isEqualTo(expectedRestaurants);
        }

        @Test
        @DisplayName("주변에 식당이 없으면 예외가 발생한다")
        void 실패_식당없음_반경1m() {
            // given
            double latitude = 37.5;
            double longitude = 127.0;
            int radius = 1;  // 1m의 좁은 반경으로 검색

            when(kakaoMapService.searchNearbyRestaurants(latitude, longitude, radius))
                    .thenThrow(new NoNearbyRestaurantsException("Can't found any around restaurant"));

            // when & then
            assertThrows(NoNearbyRestaurantsException.class, () ->
                    kakapMapGameService.searchAndPrepareRestaurants(latitude, longitude, radius));
        }
    }

    private Restaurant createTestRestaurant(Long id) {
        return Restaurant.builder()
                .name("식당" + id)
                .category("한식")
                .distance(100)
                .build();
    }

    private List<Restaurant> createTestRestaurants(int count) {
        List<Restaurant> restaurants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            restaurants.add(Restaurant.builder()
                    .name("식당" + (i + 1))
                    .category(i % 2 == 0 ? "한식" : "일식")
                    .build());
        }
        return restaurants;
    }

    private Game createTestGame(List<Restaurant> restaurants) {
        Game game = Game.builder()
                .totalRounds(16)
                .build();

        for (int i = 0; i < restaurants.size(); i += 2) {
            game.addMatch(Match.builder()
                    .restaurant1(restaurants.get(i))
                    .restaurant2(restaurants.get(i + 1))
                    .round(16)
                    .matchOrder((i / 2) + 1)
                    .build());
        }

        return game;
    }
}
