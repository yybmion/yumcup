package mioneF.yumCup.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @jakarta.persistence.Index(name = "idx_kakao_id", columnList = "kakaoId", unique = true))
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기본 정보
    private String name;
    private String category;
    private Integer distance;
    private Integer winCount;
    private Integer playCount;

    // 카카오맵 기본 정보
    @Column(unique = true)
    private String kakaoId;

    private Double latitude;
    private Double longitude;
    private String address;
    private String roadAddress;
    private String phone;
    private String placeUrl;

    // 구글 Places API 정보
    private Double rating;          // 구글 평점
    private Integer ratingCount;    // 구글 평점 개수

    private Integer priceLevel;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;        // 구글 이미지 URL

    private Boolean isOpenNow;

    private LocalDateTime updatedAt;

    @Builder(toBuilder = true)
    public Restaurant(
            String name,
            String category,
            Integer distance,
            Integer winCount,
            Integer playCount,
            String kakaoId,
            Double latitude,
            Double longitude,
            String address,
            String roadAddress,
            String phone,
            String placeUrl,
            Double rating,
            Integer ratingCount,
            Integer priceLevel,
            String photoUrl,
            Boolean isOpenNow
    ) {
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.winCount = winCount != null ? winCount : 0;
        this.playCount = playCount != null ? playCount : 0;
        this.kakaoId = kakaoId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.priceLevel = priceLevel;
        this.photoUrl = photoUrl;
        this.isOpenNow = isOpenNow;
    }

    @PreUpdate
    @PrePersist
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // KakapMapGameService에서 사용
//    public void updateWithNewInfo(Restaurant newInfo) {
//        this.distance = newInfo.getDistance();
//        this.rating = newInfo.getRating();
//        this.ratingCount = newInfo.getRatingCount();
//        this.photoUrl = newInfo.getPhotoUrl();
//        this.priceLevel = newInfo.getPriceLevel();
//        this.isOpenNow = newInfo.getIsOpenNow();
//    }
//
//    public void updateWithGoogleInfo(GooglePlaceResponse googleResponse) {
//        if (googleResponse != null &&
//                googleResponse.candidates() != null &&
//                !googleResponse.candidates().isEmpty()) {
//
//            GooglePlaceResponse.GooglePlace place = googleResponse.candidates().get(0);
//
//            // 평점 정보 업데이트
//            this.rating = place.rating();
//            this.ratingCount = place.user_ratings_total();
//
//            // 가격대 정보 업데이트
//            this.priceLevel = place.price_level();
//
//            // 영업시간 정보 업데이트
//            if (place.opening_hours() != null) {
//                this.isOpenNow = place.opening_hours().open_now();
//                if (place.opening_hours().weekday_text() != null &&
//                        !place.opening_hours().weekday_text().isEmpty()) {
//                    this.openingHours = String.join("\n", place.opening_hours().weekday_text());
//                }
//            }
//        }
//    }

    // 게임 관련 메서드
    public void incrementWinCount() {
        this.winCount++;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
