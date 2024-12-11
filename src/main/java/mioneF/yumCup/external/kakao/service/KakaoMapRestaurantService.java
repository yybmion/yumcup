package mioneF.yumCup.external.kakao.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import mioneF.yumCup.external.kakao.dto.KakaoSearchResponse;
import mioneF.yumCup.performance.Monitored;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoMapRestaurantService {
    private final WebClient kakaoWebClient;
    private final GooglePlaceService googlePlaceService;

    public KakaoMapRestaurantService(
            @Qualifier("kakaoWebClient") WebClient kakaoWebClient,
            GooglePlaceService googlePlaceService) {
        this.kakaoWebClient = kakaoWebClient;
        this.googlePlaceService = googlePlaceService;
    }

    private static final String CATEGORY_GROUP_CODE = "FD6";
    private static final int PAGE_SIZE = 15;
    private static final int REQUIRED_RESTAURANTS = 45;

    @Monitored
    public List<Restaurant> searchNearbyRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> allRestaurants = new ArrayList<>();
        int page = 1;

        while (allRestaurants.size() < REQUIRED_RESTAURANTS) {
            KakaoSearchResponse response = fetchRestaurantsPage(latitude, longitude, radius, page);

            if (response == null || response.documents().isEmpty()) {
                break;
            }

            List<Restaurant> pageRestaurants = response.documents().stream()
                    .map(doc -> convertToRestaurant(doc, latitude, longitude))
                    .collect(Collectors.toList());

            allRestaurants.addAll(pageRestaurants);

            if (response.meta().is_end()) {
                break;
            }

            page++;
        }

        if (allRestaurants.size() < 16) {
            throw new InsufficientRestaurantsException(
                    String.format("Need at least 16 restaurants, but found only %d", allRestaurants.size())
            );
        }

        Collections.shuffle(allRestaurants);
        return allRestaurants;
    }

    private KakaoSearchResponse fetchRestaurantsPage(Double latitude, Double longitude, Integer radius, int page) {
        return kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/category.json")
                        .queryParam("category_group_code", CATEGORY_GROUP_CODE)
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .queryParam("radius", radius)
                        .queryParam("size", PAGE_SIZE)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(KakaoSearchResponse.class)
                .block();
    }

    private Restaurant convertToRestaurant(KakaoDocument doc, Double userLat, Double userLng) {
        String category = extractMainCategory(doc.category_name());

        Restaurant restaurant = Restaurant.builder()
                .name(doc.place_name())
                .category(category)
                .distance(Integer.parseInt(doc.distance()))
                .kakaoId(doc.id())
                .latitude(Double.parseDouble(doc.y()))
                .longitude(Double.parseDouble(doc.x()))
                .address(doc.address_name())
                .roadAddress(doc.road_address_name())
                .phone(doc.phone())
                .placeUrl(doc.place_url())
                .build();

        try {
            GooglePlaceResponse googleResponse = googlePlaceService.findPlace(
                    doc.id(),
                    doc.place_name(),
                    Double.parseDouble(doc.y()),
                    Double.parseDouble(doc.x())
            );

            if (googleResponse != null &&
                    googleResponse.candidates() != null &&
                    !googleResponse.candidates().isEmpty()) {

                GooglePlaceResponse.GooglePlace place = googleResponse.candidates().get(0);
                String photoUrl = null;
                if (place.photos() != null && !place.photos().isEmpty()) {
                    photoUrl = googlePlaceService.getPhotoUrl(
                            place.photos().get(0).photo_reference()
                    );
                }

                // opening_hours 처리
                String weekdayText = null;
                Boolean isOpenNow = null;
                if (place.opening_hours() != null) {
                    if (place.opening_hours().weekday_text() != null && !place.opening_hours().weekday_text()
                            .isEmpty()) {
                        weekdayText = String.join("\n", place.opening_hours().weekday_text());
                    }
                    isOpenNow = place.opening_hours().open_now();
                }

                restaurant = restaurant.toBuilder()
                        .rating(place.rating())
                        .ratingCount(place.user_ratings_total())
                        .photoUrl(photoUrl)
                        .priceLevel(place.price_level())  // priceRange 대신 priceLevel 사용
                        .weekdayText(weekdayText)         // 영업시간 정보 추가
                        .isOpenNow(isOpenNow)             // 현재 영업 여부 추가
                        .build();
            }
        } catch (Exception e) {
            log.error("Error updating restaurant with Google data: ", e);
        }

        return restaurant;
    }

    private String extractMainCategory(String categoryName) {
        String[] categories = categoryName.split(" > ");
        return categories.length > 1 ? categories[1] : categories[0];
    }
}