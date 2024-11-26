package mioneF.yumCup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.request.SelectWinnerRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.external.kakao.service.KakapMapGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/yumcup")
public class YumcupController {
    private final KakapMapGameService kakapMapGameService;

    @PostMapping("/select")
    public ResponseEntity<MatchResult> selectWinner(@RequestBody SelectWinnerRequest request) {
        MatchResult response = kakapMapGameService.selectWinner(request.gameId(), request.matchId(),
                request.winnerId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start/location")
    public ResponseEntity<GameResponse> startLocationBasedGame(@RequestBody LocationRequest request) {
        log.info("Received location request: {}", request);
        try {
            GameResponse response = kakapMapGameService.startLocationBasedGame(request);
            log.info("Successfully started location based game");
            return ResponseEntity.ok(response);
        } catch (InsufficientRestaurantsException e) {
            log.warn("Not enough restaurants found");
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to start location based game", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
