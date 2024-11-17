package mioneF.yumCup.service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mioneF.yumCup.domain.Game;
import mioneF.yumCup.domain.GameResponse;
import mioneF.yumCup.domain.Match;
import mioneF.yumCup.domain.Restaurant;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final GameRepository gameRepository;

    @PostConstruct
    @Transactional
    public void initDummyData() {
        if (restaurantRepository.count() == 0) {
            List<Restaurant> dummyRestaurants = Arrays.asList(
                    Restaurant.builder()
                            .name("맛있는 김치찌개")
                            .category("한식")
                            .distance(150)
                            .imageUrl("/images/kimchi-stew.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("홍콩반점")
                            .category("중식")
                            .distance(320)
                            .imageUrl("/images/chinese-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("마라황궁")
                            .category("중식")
                            .distance(450)
                            .imageUrl("/images/mala-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("스시히로")
                            .category("일식")
                            .distance(280)
                            .imageUrl("/images/sushi-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("파스타공방")
                            .category("양식")
                            .distance(200)
                            .imageUrl("/images/pasta-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("빅맘스버거")
                            .category("양식")
                            .distance(350)
                            .imageUrl("/images/burger-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("황금돈까스")
                            .category("일식")
                            .distance(180)
                            .imageUrl("/images/tonkatsu-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("베트남쌀국수")
                            .category("아시안")
                            .distance(420)
                            .imageUrl("/images/pho-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("할매순대국")
                            .category("한식")
                            .distance(250)
                            .imageUrl("/images/sundae-soup.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("화덕피자")
                            .category("양식")
                            .distance(380)
                            .imageUrl("/images/pizza-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("춘천닭갈비")
                            .category("한식")
                            .distance(300)
                            .imageUrl("/images/dakgalbi-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("소담돈부리")
                            .category("일식")
                            .distance(220)
                            .imageUrl("/images/donburi-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("왕십리곱창")
                            .category("한식")
                            .distance(480)
                            .imageUrl("/images/gopchang-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("타이레스토랑")
                            .category("아시안")
                            .distance(400)
                            .imageUrl("/images/thai-restaurant.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("양평해장국")
                            .category("한식")
                            .distance(160)
                            .imageUrl("/images/hangover-soup.jpg")
                            .build(),

                    Restaurant.builder()
                            .name("멕시칸타코")
                            .category("양식")
                            .distance(520)
                            .imageUrl("/images/mexican-restaurant.jpg")
                            .build()
            );

            restaurantRepository.saveAll(dummyRestaurants);
        }
    }

    public GameResponse startGame() {
        // 1. 랜덤으로 16개 음식점 선택
        List<Restaurant> restaurants = restaurantRepository.findAll();
        Collections.shuffle(restaurants);
        List<Restaurant> selected = restaurants.stream()
                .limit(16)
                .collect(Collectors.toList());

        // 2. 새 게임 생성
        Game game = Game.builder()
                .totalRounds(16)
                .build();

        // 3. 16강 매치들 생성 및 저장
        for (int i = 0; i < selected.size(); i += 2) {
            Match match = Match.builder()
                    .restaurant1(selected.get(i))
                    .restaurant2(selected.get(i + 1))
                    .round(16)
                    .matchOrder(i / 2 + 1)
                    .build();

            game.addMatch(match);
        }

        Game savedGame = gameRepository.save(game);

        // 4. 첫 매치 정보와 함께 반환
        return new GameResponse(savedGame.getId(), 16,
                savedGame.getMatches().get(0),
                savedGame.getMatches().size(),
                savedGame.getStatus());
    }
}
