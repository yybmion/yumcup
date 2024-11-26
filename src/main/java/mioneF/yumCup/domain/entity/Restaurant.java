package mioneF.yumCup.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mioneF.yumCup.external.kakao.dto.KakaoPlaceResponse;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private String kakaoId;
    private Double latitude;
    private Double longitude;
    private String address;
    private String roadAddress;
    private String phone;
    private String placeUrl;
    private String openingHours;
    private String priceRange;      // 임시로 "만원-2만원" 고정값 사용

    // 구글 Places API 정보
    private Double rating;          // 구글 평점
    private Integer ratingCount;    // 구글 평점 개수

    @Column(columnDefinition = "TEXT")
    private String photoUrl;        // 구글 이미지 URL

    @Builder(toBuilder = true)
    public Restaurant(
            String name,
            String category,
            Integer distance,
            String kakaoId,
            Double latitude,
            Double longitude,
            String address,
            String roadAddress,
            String phone,
            String placeUrl,
            String openingHours,
            String priceRange,
            Double rating,
            Integer ratingCount,
            String photoUrl
    ) {
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.kakaoId = kakaoId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.openingHours = openingHours;
        this.priceRange = priceRange;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.photoUrl = photoUrl;
        this.winCount = 0;
        this.playCount = 0;
    }

    // 카카오 API 정보로 업데이트
    public void updateWithKakaoDetail(KakaoPlaceResponse detail) {
        if (detail != null) {
            this.openingHours = detail.opening_hours();
            this.priceRange = "만원-2만원";  // 임시 고정값
        }
    }

    // KakapMapGameService에서 사용
    public void updateWithNewInfo(Restaurant newInfo) {
        this.distance = newInfo.getDistance();
        this.rating = newInfo.getRating();
        this.ratingCount = newInfo.getRatingCount();
        this.photoUrl = newInfo.getPhotoUrl();
    }

    // 게임 관련 메서드
    public void incrementWinCount() {
        this.winCount++;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }
}
