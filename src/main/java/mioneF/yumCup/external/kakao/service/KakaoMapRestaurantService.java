package mioneF.yumCup.external.kakao.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.exception.ExternalApiException;
import mioneF.yumCup.exception.InsufficientRestaurantsException;
import mioneF.yumCup.exception.NoNearbyRestaurantsException;
import mioneF.yumCup.exception.RestaurantNotFoundException;
import mioneF.yumCup.exception.RestaurantProcessingException;
import mioneF.yumCup.exception.RestaurantProcessingTimeoutException;
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
    private static final String CATEGORY_GROUP_CODE = "FD6";
    private static final int PAGE_SIZE = 15;
    private static final int REQUIRED_RESTAURANTS = 16;
    private static final int TIMEOUT_SECONDS = 5;

    private final ExecutorService executorService;
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
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2
        );
    }

    @Monitored
    public List<Restaurant> searchNearbyRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> allRestaurants = new ArrayList<>();
        int page = 1;

        while (allRestaurants.size() < REQUIRED_RESTAURANTS) {
            KakaoSearchResponse response = fetchRestaurantsPage(latitude, longitude, radius, page);

            if (response == null || response.documents().isEmpty()) {
                throw new NoNearbyRestaurantsException(
                        String.format("Can't found any around restaurant")
                );
            }

            // CompletableFuture 리스트 생성
            List<CompletableFuture<Restaurant>> futures = response.documents().stream()
                    .map(doc -> CompletableFuture.supplyAsync(() -> {
                        return transactionTemplate.execute(status -> {
                            try {
                                // 1. 먼저 DB에서 찾기
                                Optional<Restaurant> existingRestaurant = restaurantRepository.findByKakaoId(doc.id());
                                if (existingRestaurant.isPresent()) {
                                    log.info("Found existing restaurant: {}", doc.place_name());
                                    if (existingRestaurant.get().getUpdatedAt()
                                            .isAfter(LocalDateTime.now().minusDays(14))) {
                                        return existingRestaurant.get();
                                    }

                                    Restaurant restaurant = createRestaurantWithGoogleInfo(doc);
                                    existingRestaurant.get().updateWithNewInfo(restaurant);
                                    return existingRestaurant.get();
                                }

                                // 2. 구글 API 호출하여 레스토랑 정보 생성
                                Restaurant newRestaurant = createRestaurantWithGoogleInfo(doc);

                                try {
                                    // 3. 저장 시도
                                    return restaurantRepository.save(newRestaurant);
                                } catch (DataIntegrityViolationException e) {
                                    // 4. 저장 실패시 (다른 쓰레드가 이미 저장한 경우) 다시 조회
                                    log.info("Restaurant was already saved by another thread: {}", doc.place_name());
                                    return restaurantRepository.findByKakaoId(doc.id())
                                            .orElseThrow(() -> new RestaurantNotFoundException("Failed to process restaurant: " + doc.id()));
                                }
                            } catch (Exception e) {
                                log.error("Error processing restaurant {}: {}", doc.place_name(), e.getMessage());
                                status.setRollbackOnly();
                                throw e;
                            }
                        });
                    }, executorService))
                    .collect(Collectors.toList());

            // 모든 Future 완료 대기
            List<Restaurant> pageRestaurants = collectRestaurantResults(futures);

            allRestaurants.addAll(pageRestaurants);

            if (response.meta().is_end()) {
                break;
            }

            page++;
        }

        if (allRestaurants.size() < REQUIRED_RESTAURANTS) {
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

    private Restaurant createRestaurantWithGoogleInfo(KakaoDocument doc) {
        // 1. 카카오 데이터로 기본 레스토랑 생성
        Restaurant restaurant = createBaseRestaurant(doc);

        try {
            // 2. 구글 장소 정보 조회
            GooglePlaceResponse googleResponse = googlePlaceService.findPlace(
                    doc.id(),
                    doc.place_name(),
                    Double.parseDouble(doc.y()),
                    Double.parseDouble(doc.x())
            );

            // 3. 구글 정보로 레스토랑 정보 보강
            if (isValidGoogleResponse(googleResponse)) {
                GooglePlaceResponse.GooglePlace place = googleResponse.candidates().get(0);
                restaurant = updateRestaurantWithGoogleInfo(restaurant, place);
            }
        } catch (Exception e) {
            throw new ExternalApiException("Error updating restaurant with Google data:", e);
        }

        return restaurant;
    }

    // 카카오 데이터로 기본 레스토랑 정보 생성
    private Restaurant createBaseRestaurant(KakaoDocument doc) {
        String category = extractMainCategory(doc.category_name());

        return Restaurant.builder()
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
    }

    // 구글 응답이 유효한지 검증
    private boolean isValidGoogleResponse(GooglePlaceResponse googleResponse) {
        return googleResponse != null &&
                googleResponse.candidates() != null &&
                !googleResponse.candidates().isEmpty();
    }

    // 구글 정보로 레스토랑 정보 업데이트
    private Restaurant updateRestaurantWithGoogleInfo(Restaurant restaurant,
                                                      GooglePlaceResponse.GooglePlace place) {
        String photoUrl = extractPhotoUrl(place);
        Boolean isOpenNow = extractOpeningStatus(place);

        return restaurant.toBuilder()
                .rating(place.rating())
                .ratingCount(place.user_ratings_total())
                .photoUrl(photoUrl)
                .priceLevel(place.price_level())
                .isOpenNow(isOpenNow)
                .build();
    }

    // 구글 장소의 사진 URL 추출
    private String extractPhotoUrl(GooglePlaceResponse.GooglePlace place) {
        if (place.photos() != null && !place.photos().isEmpty()) {
            return googlePlaceService.getPhotoUrl(
                    place.photos().get(0).photo_reference()
            );
        }
        return null;
    }

    // 구글 장소의 영업 상태 추출
    private Boolean extractOpeningStatus(GooglePlaceResponse.GooglePlace place) {
        if (place.opening_hours() != null) {
            return place.opening_hours().open_now();
        }
        return null;
    }

    private String extractMainCategory(String categoryName) {
        String[] categories = categoryName.split(" > ");
        return categories.length > 1 ? categories[1] : categories[0];
    }

    private List<Restaurant> collectRestaurantResults(List<CompletableFuture<Restaurant>> futures) {
        return futures.stream()
                .map(this::getRestaurantWithTimeout)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Restaurant getRestaurantWithTimeout(CompletableFuture<Restaurant> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RestaurantProcessingTimeoutException("Restaurant information processing timeout");
        } catch (Exception e) {
            throw new RestaurantProcessingException("Error processing restaurant information");
        }
    }
}
