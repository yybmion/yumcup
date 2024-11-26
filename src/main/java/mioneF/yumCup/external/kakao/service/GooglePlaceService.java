package mioneF.yumCup.external.kakao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.cache.GooglePlaceCache;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@CacheConfig(cacheNames = "googlePlaces")
public class GooglePlaceService {
    private final WebClient googleWebClient;
    private final String googleApiKey;
    private final RedisTemplate<String, GooglePlaceCache> redisTemplate;

    private static final int CACHE_DURATION_HOURS = 24;
    private static final int MAX_PHOTO_WIDTH = 400;
    private static final String CACHE_KEY_FORMAT = "place:%s:%s";
    private static final String PHOTO_URL_FORMAT =
            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=%d&photo_reference=%s&key=%s";

    public GooglePlaceService(
            @Qualifier("googleWebClient") WebClient googleWebClient,
            @Value("${google.api.key}") String googleApiKey,
            RedisTemplate<String, GooglePlaceCache> redisTemplate) {
        this.googleWebClient = googleWebClient;
        this.googleApiKey = googleApiKey;
        this.redisTemplate = redisTemplate;
    }

    public GooglePlaceResponse findPlace(String kakaoId, String name, double lat, double lng) {
        String cacheKey = generateCacheKey(kakaoId, name);
        log.info("Searching place info for: {} (kakaoId: {})", name, kakaoId);

        // 1. Try to get from cache
        GooglePlaceCache cachedPlace = getFromCache(cacheKey);
        if (cachedPlace != null) {
            log.info("Cache hit for: {} (kakaoId: {})", name, kakaoId);
            return createResponseFromCache(cachedPlace);
        }

        // 2. If not in cache, fetch from API
        log.info("Cache miss for: {} (kakaoId: {}), fetching from Google API", name, kakaoId);
        return fetchAndCachePlace(name, lat, lng, cacheKey);
    }

    private GooglePlaceCache getFromCache(String cacheKey) {
        try {
            GooglePlaceCache cachedPlace = redisTemplate.opsForValue().get(cacheKey);
            if (cachedPlace != null) {
                log.debug("Found in cache: {}", cacheKey);
                return cachedPlace;
            }
        } catch (Exception e) {
            log.error("Error accessing cache: {}", e.getMessage());
        }
        return null;
    }

    private GooglePlaceResponse createResponseFromCache(GooglePlaceCache cache) {
        return new GooglePlaceResponse(
                List.of(new GooglePlaceResponse.GooglePlace(
                        cache.id(),
                        cache.name(),
                        cache.rating(),
                        cache.ratingCount(),
                        cache.photoUrl() != null ?
                                List.of(new GooglePlaceResponse.GooglePlace.Photo(cache.photoUrl(), null, null))
                                : null
                )),
                "OK"
        );
    }

    private GooglePlaceResponse fetchAndCachePlace(String name, double lat, double lng, String cacheKey) {
        try {
            GooglePlaceResponse response = fetchFromGoogleApi(name, lat, lng);
            if (response != null) {
                // ZERO_RESULTS인 경우에도 캐시
                if (response.status().equals("ZERO_RESULTS")) {
                    cacheEmptyResult(cacheKey, name);
                    log.info("Caching ZERO_RESULTS for: {}", name);
                    return response;
                }
                // 기존 로직: 결과가 있는 경우
                else if (!response.candidates().isEmpty()) {
                    GooglePlaceResponse.GooglePlace place = response.candidates().get(0);
                    cachePlace(cacheKey, place, name);
                    log.info("Successfully fetched and cached place data for: {}", name);
                    return response;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching place details for {}: {}", name, e.getMessage());
        }
        return null;
    }

    private void cachePlace(String cacheKey, GooglePlaceResponse.GooglePlace place, String name) {
        String photoUrl = null;
        if (place.photos() != null && !place.photos().isEmpty()) {
            photoUrl = generatePhotoUrl(place.photos().get(0).photo_reference());
        }

        GooglePlaceCache cacheEntry = new GooglePlaceCache(
                cacheKey,
                place.rating(),
                place.user_ratings_total(),
                photoUrl,
                LocalDateTime.now(),
                name
        );

        try {
            redisTemplate.opsForValue().set(cacheKey, cacheEntry, CACHE_DURATION_HOURS, TimeUnit.HOURS);
            log.debug("Cached place data for key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Error caching place data: {}", e.getMessage());
        }
    }

    private void cacheEmptyResult(String cacheKey, String name) {
        GooglePlaceCache cacheEntry = new GooglePlaceCache(
                cacheKey,
                0.0,           // 기본 평점
                0,             // 기본 평점 개수
                null,          // 사진 없음
                LocalDateTime.now(),
                name
        );

        try {
            redisTemplate.opsForValue().set(cacheKey, cacheEntry, CACHE_DURATION_HOURS, TimeUnit.HOURS);
            log.debug("Cached empty result for key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Error caching empty result: {}", e.getMessage());
        }
    }

    private GooglePlaceResponse fetchFromGoogleApi(String name, double lat, double lng) throws Exception {
        try {
            String response = googleWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/findplacefromtext/json")
                            .queryParam("input", name)
                            .queryParam("inputtype", "textquery")
                            .queryParam("locationbias", String.format("circle:100@%f,%f", lat, lng))
                            .queryParam("fields", "place_id,name,rating,user_ratings_total,photos")
                            .queryParam("key", googleApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            GooglePlaceResponse googleResponse = new ObjectMapper().readValue(response, GooglePlaceResponse.class);
            log.info("Google API response for {}: status={}, candidates={}",
                    name,
                    googleResponse.status(),
                    googleResponse.candidates() != null ? googleResponse.candidates().size() : 0);
            return googleResponse;
        } catch (Exception e) {
            log.error("Failed to fetch from Google API for {}: {}", name, e.getMessage());
            throw e;
        }
    }

    public String getPhotoUrl(String photoReference) {
        if (photoReference == null) {
            log.warn("Received null photo reference");
            return null;
        }

        if (photoReference.startsWith("https://")) {
            log.debug("Using cached photo URL");
            return photoReference;
        }

        log.debug("Generating photo URL for reference: {}", photoReference);
        return generatePhotoUrl(photoReference);
    }

    private String generatePhotoUrl(String photoReference) {
        return String.format(PHOTO_URL_FORMAT, MAX_PHOTO_WIDTH, photoReference, googleApiKey);
    }

    private String generateCacheKey(String kakaoId, String name) {
        return String.format(CACHE_KEY_FORMAT, kakaoId, name);
    }
}

