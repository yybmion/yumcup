package mioneF.yumCup.service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mioneF.yumCup.domain.Restaurant;
import mioneF.yumCup.domain.RestaurantResponse;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;

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

    public List<RestaurantResponse> startGame() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        Collections.shuffle(restaurants);
        List<Restaurant> selected = restaurants.stream()
                .limit(16)
                .collect(Collectors.toList());

        return selected.stream()
                .map(RestaurantResponse::from)
                .collect(Collectors.toList()
                );
    }
}
