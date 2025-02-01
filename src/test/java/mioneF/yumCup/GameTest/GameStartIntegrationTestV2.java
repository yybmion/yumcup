package mioneF.yumCup.GameTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
public class GameStartIntegrationTestV2 {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("위치 기반 게임 시작 테스트")
    class StartLocationBasedGameTest {

        /**
         * 실제 외부 api 요청
         */
        @Test
        @DisplayName("정상적인 위치 요청으로 게임 시작")
        void 성공_게임시작_유효한위치요청() throws Exception {
            // given
            LocationRequest request = new LocationRequest(37.390759, 126.953572, 500);
            String jsonContent = objectMapper.writeValueAsString(request);

            // when & then
            mockMvc.perform(post("/api/yumcup/start/location")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonContent))
                    .andExpectAll(
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
    }
}
