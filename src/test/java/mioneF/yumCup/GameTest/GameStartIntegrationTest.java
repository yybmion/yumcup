package mioneF.yumCup.GameTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.GameStatus;
import mioneF.yumCup.exception.ExternalApiException;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.exception.RestaurantProcessingTimeoutException;
import mioneF.yumCup.external.kakao.service.KakapMapGameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
public class GameStartIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KakapMapGameService kakapMapGameService;

    @Nested
    @DisplayName("위치 기반 게임 시작 테스트")
    class StartLocationBasedGameTest {

        @Test
        @DisplayName("정상적인 위치 요청으로 게임 시작")
        void 성공_게임시작_유효한위치요청() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 500);
            String jsonContent = objectMapper.writeValueAsString(request);

            GameResponse mockResponse = makeMockGameResponse();

            when(kakapMapGameService.startLocationBasedGame(request))
                    .thenReturn(mockResponse);

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpectAll(
                            // 실제 값으로 검증
                            jsonPath("$.gameId").value(1L),
                            jsonPath("$.currentRound").value(16),
                            jsonPath("$.status").value("PROGRESS"),
                            jsonPath("$.currentMatch.id").value(1L),
                            jsonPath("$.currentMatch.round").value(16),
                            jsonPath("$.currentMatch.matchOrder").value(1),
                            // 복잡한 객체는 존재 여부만 확인
                            jsonPath("$.currentMatch.restaurant1").exists(),
                            jsonPath("$.currentMatch.restaurant2").exists()
                    );
        }

        @Test
        @DisplayName("주변에 음식점이 없는 경우")
        void 실패_게임시작_주변반경1로설정() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 1);
            String jsonContent = objectMapper.writeValueAsString(request);

            when(kakapMapGameService.startLocationBasedGame(request))
                    .thenThrow(new NoNearbyRestaurantsException("Can't found any around restaurant"));

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Can't found any around restaurant"));
        }

        @Test
        @DisplayName("Google API 호출 실패")
        void 실패_게임시작_구글api호출실패() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 500);
            String jsonContent = objectMapper.writeValueAsString(request);

            when(kakapMapGameService.startLocationBasedGame(request))
                    .thenThrow(new ExternalApiException(
                            "Error updating restaurant with Google data:",
                            new RuntimeException("Google API Error")
                    ));

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Error updating restaurant with Google data:"));
        }

        @Test
        @DisplayName("검색된 레스토랑 수가 부족한 경우")
        void 실패_게임시작_주변레스토랑부족() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 500);
            String jsonContent = objectMapper.writeValueAsString(request);

            // 실제로 10개의 레스토랑만 찾았다고 가정
            when(kakapMapGameService.startLocationBasedGame(request))
                    .thenThrow(new InsufficientRestaurantsException(
                            String.format("Need at least 16 restaurants, but found only %d", 10)
                    ));

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Need at least 16 restaurants, but found only 10"));
        }

        @Test
        @DisplayName("외부 API호출시간에 대해 타임아웃된 경우")
        void 실패_게임시작_api호출타임아웃() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 500);
            String jsonContent = objectMapper.writeValueAsString(request);

            when(kakapMapGameService.startLocationBasedGame(request))
                    .thenThrow(new RestaurantProcessingTimeoutException("Restaurant information processing timeout"));

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpect(status().isRequestTimeout())
                    .andExpect(jsonPath("$.message")
                            .value("Restaurant information processing timeout"));
        }
    }

    private GameResponse makeMockGameResponse() {
        RestaurantResponse restaurant1 = new RestaurantResponse(
                1L,
                "맛있는 식당",
                "한식",
                100,
                "서울시 강남구",
                "강남대로 123",
                "02-123-4567",
                "http://place.url",
                "http://photo.url",
                4.5,
                100,
                "보통",
                true
        );

        RestaurantResponse restaurant2 = new RestaurantResponse(
                2L,
                "맛있는 식당2",
                "일식",
                150,
                "서울시 강남구",
                "강남대로 456",
                "02-123-4568",
                "http://place2.url",
                "http://photo2.url",
                4.2,
                80,
                "보통",
                true
        );

        MatchResponse matchResponse = new MatchResponse(
                1L,
                restaurant1,
                restaurant2,
                16,
                1
        );

        GameResponse mockResponse = new GameResponse(
                1L,
                16,
                matchResponse,
                GameStatus.PROGRESS
        );

        return mockResponse;
    }
}
