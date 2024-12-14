package mioneF.yumCup.external.kakao.service;

import com.google.common.collect.Lists;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.MatchResult;
import mioneF.yumCup.domain.dto.request.LocationRequest;
import mioneF.yumCup.domain.dto.response.GameResponse;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.dto.response.MatchResponse;
import mioneF.yumCup.domain.dto.response.RestaurantResponse;
import mioneF.yumCup.domain.entity.Game;
import mioneF.yumCup.domain.entity.Match;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.repository.GameRepository;
import mioneF.yumCup.repository.RestaurantRepository;
import mioneF.yumCup.service.GameService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class KakapMapGameService {
//    private final ExecutorService executorService = Executors.newFixedThreadPool(
//            Runtime.getRuntime().availableProcessors() * 2
//    );
//    private static final int BATCH_SIZE = 8;

    private final KakaoMapRestaurantService kakaoMapService;
    private final GooglePlaceService googlePlaceService;
    private final GameService gameService;
    private final RestaurantRepository restaurantRepository;
    private final GameRepository gameRepository;
    private final TransactionTemplate transactionTemplate;
    private final RetryTemplate retryTemplate;

    public KakapMapGameService(KakaoMapRestaurantService kakaoMapService, GooglePlaceService googlePlaceService,
                               GameService gameService, RestaurantRepository restaurantRepository,
                               GameRepository gameRepository, TransactionTemplate transactionTemplate,
                               RetryTemplate retryTemplate) {
        this.kakaoMapService = kakaoMapService;
        this.googlePlaceService = googlePlaceService;
        this.gameService = gameService;
        this.restaurantRepository = restaurantRepository;
        this.gameRepository = gameRepository;
        this.transactionTemplate = transactionTemplate;
        this.retryTemplate = retryTemplate;
    }

    public MatchResult selectWinner(Long gameId, Long matchId, Long winnerId) {
        return gameService.selectWinner(gameId, matchId, winnerId);
    }

    public GameResponse startLocationBasedGame(LocationRequest request) {
        try {
            // 1) API 호출 부분 분리 - 트랜잭션 없이 실행
            List<Restaurant> restaurants = searchAndPrepareRestaurants(
                    request.latitude(),
                    request.longitude(),
                    request.radius()
            );

            // 2) DB 작업만 트랜잭션으로 처리
            return createGameWithRestaurants(restaurants);
        } catch (Exception e) {
            log.error("Error in startLocationBasedGame: ", e);
            throw new RuntimeException("Failed to start game: " + e.getMessage());
        }
    }

    public List<Restaurant> searchAndPrepareRestaurants(Double latitude, Double longitude, Integer radius) {
        List<Restaurant> kakaoRestaurants = kakaoMapService.searchNearbyRestaurants(latitude, longitude, radius);
        return kakaoRestaurants;
    }

    // DB 작업 부분 - 트랜잭션 처리
    @Transactional
    public GameResponse createGameWithRestaurants(List<Restaurant> restaurants) {
        // 이미 저장된 Restaurant들을 사용하므로 추가 저장 없이 진행
        List<Restaurant> selectedRestaurants = restaurants.stream()
                .limit(16)
                .collect(Collectors.toList());

        Game game = Game.builder()
                .totalRounds(16)
                .build();

        for (int i = 0; i < selectedRestaurants.size(); i += 2) {
            Match match = Match.builder()
                    .restaurant1(selectedRestaurants.get(i))
                    .restaurant2(selectedRestaurants.get(i + 1))
                    .round(16)
                    .matchOrder((i / 2) + 1)
                    .build();

            game.addMatch(match);
        }

        Game savedGame = gameRepository.save(game);
        Match firstMatch = savedGame.getMatches().get(0);

        return new GameResponse(
                savedGame.getId(),
                16,
                new MatchResponse(
                        firstMatch.getId(),
                        RestaurantResponse.from(firstMatch.getRestaurant1()),
                        RestaurantResponse.from(firstMatch.getRestaurant2()),
                        firstMatch.getRound(),
                        firstMatch.getMatchOrder()
                ),
                savedGame.getStatus()
        );
    }

//    private List<Restaurant> processRestaurantsInBatches(List<Restaurant> kakaoRestaurants) {
//        return Lists.partition(kakaoRestaurants, BATCH_SIZE).stream()
//                .map(batch -> processBatchWithoutTransaction(batch))
//                .flatMap(Collection::stream)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    private List<Restaurant> processBatchWithoutTransaction(List<Restaurant> batch) {
//        List<CompletableFuture<Restaurant>> futures = batch.stream()
//                .map(restaurant -> CompletableFuture.supplyAsync(
//                        () -> processRestaurantWithTransaction(restaurant),
//                        executorService
//                ))
//                .collect(Collectors.toList());
//
//        try {
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
//                    .get(10, TimeUnit.SECONDS);
//
//            return futures.stream()
//                    .map(this::getResultWithTimeout)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            log.error("Error processing batch: ", e);
//            return batch;
//        }
//    }
//
//    private Restaurant getResultWithTimeout(CompletableFuture<Restaurant> future) {
//        try {
//            return future.get(3, TimeUnit.SECONDS);
//        } catch (Exception e) {
//            log.error("Error getting future result", e);
//            return null;
//        }
//    }
//
//    @Transactional
//    public Restaurant processRestaurantWithTransaction(Restaurant restaurant) {
//        try {
//            // 1. DB에서 기존 데이터 확인
//            Optional<Restaurant> existingOpt = restaurantRepository.findByKakaoId(restaurant.getKakaoId());
//
//            // 2. 있으면 그대로 반환 (업데이트 하지 않음)
//            if (existingOpt.isPresent()) {
//                log.info("Found existing restaurant: {}", existingOpt.get().getName());
//                return existingOpt.get();
//            }
//
//            // 3. 없을 때만 Google API 호출
//            log.info("Fetching data from Google API for: {}", restaurant.getName());
//            GooglePlaceResponse googleResponse = googlePlaceService.findPlace(
//                    restaurant.getKakaoId(),
//                    restaurant.getName(),
//                    restaurant.getLatitude(),
//                    restaurant.getLongitude()
//            );
//
//            // 4. 저장 시도
//            return transactionTemplate.execute(status -> {
//                try {
//                    // 다시 한번 확인 (다른 트랜잭션이 저장했을 수 있음)
//                    Optional<Restaurant> finalCheck = restaurantRepository.findByKakaoId(restaurant.getKakaoId());
//                    if (finalCheck.isPresent()) {
//                        log.info("Restaurant was saved by another transaction: {}", finalCheck.get().getName());
//                        return finalCheck.get();
//                    }
//
//                    // 정말 없을 때만 저장
//                    restaurant.updateWithGoogleInfo(googleResponse);
//                    if (googleResponse.candidates() != null &&
//                            !googleResponse.candidates().isEmpty() &&
//                            googleResponse.candidates().get(0).photos() != null &&
//                            !googleResponse.candidates().get(0).photos().isEmpty()) {
//                        String photoUrl = googlePlaceService.getPhotoUrl(
//                                googleResponse.candidates().get(0).photos().get(0).photo_reference()
//                        );
//                        restaurant.setPhotoUrl(photoUrl);
//                    }
//
//                    return restaurantRepository.save(restaurant);
//                } catch (Exception e) {
//                    log.error("Error saving restaurant: ", e);
//                    // unique constraint violation 등이 발생하면 다시 조회
//                    return restaurantRepository.findByKakaoId(restaurant.getKakaoId())
//                            .orElseThrow(() -> new RuntimeException("Failed to process restaurant"));
//                }
//            });
//        } catch (Exception e) {
//            log.error("Error in processRestaurantWithTransaction: ", e);
//            throw new RuntimeException("Failed to process restaurant", e);
//        }
//    }
//
//
//    @PreDestroy
//    public void shutdown() {
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//        }
//    }
}
