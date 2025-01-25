package mioneF.yumCup.exception.response;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String message;        // 사용자에게 보여줄 메시지
    private final LocalDateTime timestamp;// 에러 발생 시간

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
