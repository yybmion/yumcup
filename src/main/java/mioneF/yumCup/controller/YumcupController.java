package mioneF.yumCup.controller;

import lombok.RequiredArgsConstructor;
import mioneF.yumCup.domain.GameResponse;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.service.GameService;
import mioneF.yumCup.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/yumcup")
public class YumcupController {
    private final RestaurantService restaurantService;
    private final GameService gameService;

    @GetMapping("/start")
    public ResponseEntity<GameResponse> startGame() {
        GameResponse response = restaurantService.startGame();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/select")
    public ResponseEntity<MatchResult> selectWinner(
            @RequestParam Long gameId,
            @RequestParam Long matchId,
            @RequestParam Long winnerId) {
        MatchResult result = gameService.selectWinner(gameId, matchId, winnerId);
        return ResponseEntity.ok(result);
    }
}
