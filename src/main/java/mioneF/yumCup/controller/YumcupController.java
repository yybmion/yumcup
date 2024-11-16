package mioneF.yumCup.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mioneF.yumCup.domain.RestaurantResponse;
import mioneF.yumCup.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/yumcup")
public class YumcupController {
    private final RestaurantService restaurantService;

    @GetMapping("/start")
    public ResponseEntity<List<RestaurantResponse>> startGame() {
        List<RestaurantResponse> response = restaurantService.startGame();

        return ResponseEntity.ok(response);
    }
}
