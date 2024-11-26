package mioneF.yumCup.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.time.LocalDateTime;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("googlePlace")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class GooglePlaceCache implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty
    private final String id;
    @JsonProperty
    private final Double rating;
    @JsonProperty
    private final Integer ratingCount;
    @JsonProperty
    private final String photoUrl;
    @JsonProperty
    private final LocalDateTime cachedAt;
    @JsonProperty
    private final String name;

    @JsonCreator
    public GooglePlaceCache(
            @JsonProperty("id") String id,
            @JsonProperty("rating") Double rating,
            @JsonProperty("ratingCount") Integer ratingCount,
            @JsonProperty("photoUrl") String photoUrl,
            @JsonProperty("cachedAt") LocalDateTime cachedAt,
            @JsonProperty("name") String name) {
        this.id = id;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.photoUrl = photoUrl;
        this.cachedAt = cachedAt;
        this.name = name;
    }

    // Getter methods
    public String id() { return id; }
    public Double rating() { return rating; }
    public Integer ratingCount() { return ratingCount; }
    public String photoUrl() { return photoUrl; }
    public LocalDateTime cachedAt() { return cachedAt; }
    public String name() { return name; }

    public static GooglePlaceCache from(String kakaoId, String name, GooglePlaceResponse.GooglePlace place, String photoUrl) {
        return new GooglePlaceCache(
                kakaoId,
                place.rating(),
                place.user_ratings_total(),
                photoUrl,
                LocalDateTime.now(),
                name
        );
    }
}
