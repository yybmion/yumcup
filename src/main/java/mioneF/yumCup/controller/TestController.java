package mioneF.yumCup.controller;

import java.util.List;
import mioneF.yumCup.domain.RestaurantResponse;
import mioneF.yumCup.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yumcup")
public class TestController {
    private RestaurantService restaurantService;

    @PostMapping("/start")
    public ResponseEntity<List<RestaurantResponse>> startGame() {
        List<RestaurantResponse> response = restaurantService.startGame();

        return ResponseEntity.ok(response);
    }
}
