package mioneF.yumCup.external.kakao.service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import mioneF.yumCup.external.kakao.dto.KakaoSearchResponse;
import mioneF.yumCup.performance.Monitored;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class KakaoMapRestaurantService {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );
    private static final String CATEGORY_GROUP_CODE = "FD6";
    private static final int PAGE_SIZE = 15;
    private static final int REQUIRED_RESTAURANTS = 16;
    private static final int FUTURE_TIMEOUT_SECONDS = 5;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 60;

    private final WebClient kakaoWebClient;
    private final GooglePlaceService googlePlaceService;
    private final RestaurantRepository restaurantRepository;
    private final TransactionTemplate transactionTemplate;

    public KakaoMapRestaurantService(
            @Qualifier("kakaoWebClient") WebClient kakaoWebClient,
            GooglePlaceService googlePlaceService,
            RestaurantRepository restaurantRepository,
            TransactionTemplate transactionTemplate) {
        this.kakaoWebClient = kakaoWebClient;
        this.googlePlaceService = googlePlaceService;
        this.restaurantRepository = restaurantRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Monitored
    public List<Restaurant> searchNearbyRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> allRestaurants = new ArrayList<>();
        int page = 1;

        while (allRestaurants.size() < REQUIRED_RESTAURANTS) {
            KakaoSearchResponse response = fetchRestaurantsPage(latitude, longitude, radius, page);

            if (isEmptyResponse(response)) {
                break;
            }

            List<Restaurant> pageRestaurants = processRestaurantPage(response.documents(), latitude, longitude);
            allRestaurants.addAll(pageRestaurants);

            if (response.meta().is_end()) {
                break;
            }
            page++;
        }

        validateRestaurantCount(allRestaurants);
        Collections.shuffle(allRestaurants);
        return allRestaurants;
    }

    private boolean isEmptyResponse(KakaoSearchResponse response) {
        return response == null || response.documents().isEmpty();
    }

    private List<Restaurant> processRestaurantPage(List<KakaoDocument> documents, Double latitude, Double longitude) {
        List<CompletableFuture<Restaurant>> futures = documents.stream()
                .map(doc -> createRestaurantFuture(doc, latitude, longitude))
                .collect(Collectors.toList());

        return futures.stream()
                .map(this::getRestaurantFromFuture)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CompletableFuture<Restaurant> createRestaurantFuture(KakaoDocument doc, Double latitude, Double longitude) {
        return CompletableFuture.supplyAsync(() ->
                processRestaurantWithTransaction(doc, latitude, longitude), executorService);
    }

    private Restaurant processRestaurantWithTransaction(KakaoDocument doc, Double latitude, Double longitude) {
        return transactionTemplate.execute(status -> {
            try {
                return findOrCreateRestaurant(doc, latitude, longitude);
            } catch (Exception e) {
                log.error("Error processing restaurant {}: {}", doc.place_name(), e.getMessage());
                status.setRollbackOnly();
                throw new RuntimeException("Failed to process restaurant", e);
            }
        });
    }

    private Restaurant findOrCreateRestaurant(KakaoDocument doc, Double latitude, Double longitude) {
        Optional<Restaurant> existingRestaurant = restaurantRepository.findByKakaoId(doc.id());
        if (existingRestaurant.isPresent()) {
            log.info("Found existing restaurant: {}", doc.place_name());
            return existingRestaurant.get();
        }

        Restaurant newRestaurant = createRestaurantWithGoogleInfo(doc, latitude, longitude);
        try {
            return restaurantRepository.save(newRestaurant);
        } catch (DataIntegrityViolationException e) {
            log.info("Restaurant was already saved by another thread: {}", doc.place_name());
            return restaurantRepository.findByKakaoId(doc.id())
                    .orElseThrow(() -> new RuntimeException("Failed to process restaurant"));
        }
    }

    private Restaurant getRestaurantFromFuture(CompletableFuture<Restaurant> future) {
        try {
            return future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error processing restaurant: ", e);
            return null;
        }
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

    private Restaurant createRestaurantWithGoogleInfo(KakaoDocument doc, Double userLat, Double userLng) {
        Restaurant restaurant = createBasicRestaurant(doc);
        enrichRestaurantWithGoogleInfo(restaurant, doc);
        return restaurant;
    }

    private Restaurant createBasicRestaurant(KakaoDocument doc) {
        return Restaurant.builder()
                .name(doc.place_name())
                .category(extractMainCategory(doc.category_name()))
                .distance(Integer.parseInt(doc.distance()))
                .kakaoId(doc.id())
                .latitude(Double.parseDouble(doc.y()))
                .longitude(Double.parseDouble(doc.x()))
                .address(doc.address_name())
                .roadAddress(doc.road_address_name())
                .phone(doc.phone())
                .placeUrl(doc.place_url())
                .build();
    }

    private void enrichRestaurantWithGoogleInfo(Restaurant restaurant, KakaoDocument doc) {
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
                updateRestaurantWithGoogleData(restaurant, googleResponse.candidates().get(0));
            }
        } catch (Exception e) {
            log.error("Error updating restaurant with Google data: ", e);
        }
    }

    private void updateRestaurantWithGoogleData(Restaurant restaurant, GooglePlaceResponse.GooglePlace place) {
        String photoUrl = getPhotoUrlFromPlace(place);
        GooglePlaceOpeningHours openingHours = extractOpeningHours(place);

        restaurant = restaurant.toBuilder()
                .rating(place.rating())
                .ratingCount(place.user_ratings_total())
                .photoUrl(photoUrl)
                .priceLevel(place.price_level())
                .weekdayText(openingHours.weekdayText())
                .isOpenNow(openingHours.isOpenNow())
                .build();
    }

    private String getPhotoUrlFromPlace(GooglePlaceResponse.GooglePlace place) {
        if (place.photos() != null && !place.photos().isEmpty()) {
            return googlePlaceService.getPhotoUrl(place.photos().get(0).photo_reference());
        }
        return null;
    }

    private GooglePlaceOpeningHours extractOpeningHours(GooglePlaceResponse.GooglePlace place) {
        if (place.opening_hours() != null) {
            String weekdayText = null;
            if (place.opening_hours().weekday_text() != null &&
                    !place.opening_hours().weekday_text().isEmpty()) {
                weekdayText = String.join("\n", place.opening_hours().weekday_text());
            }
            return new GooglePlaceOpeningHours(weekdayText, place.opening_hours().open_now());
        }
        return new GooglePlaceOpeningHours(null, null);
    }

    private record GooglePlaceOpeningHours(String weekdayText, Boolean isOpenNow) {}

    private String extractMainCategory(String categoryName) {
        String[] categories = categoryName.split(" > ");
        return categories.length > 1 ? categories[1] : categories[0];
    }

    private void validateRestaurantCount(List<Restaurant> restaurants) {
        if (restaurants.size() < REQUIRED_RESTAURANTS) {
            throw new InsufficientRestaurantsException(
                    String.format("Need at least %d restaurants, but found only %d",
                            REQUIRED_RESTAURANTS, restaurants.size())
            );
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
