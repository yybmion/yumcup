package mioneF.yumCup.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // 음식점 이름
    private String category;    // 음식 카테고리
    private Integer distance;   // 거리(미터)
    private String imageUrl;    // 음식점 이미지 URL
    private Integer winCount;   // 우승 횟수
    private Integer playCount;  // 게임 참여 횟수

    // 카카오맵 연동을 위한 필드들
    private String kakaoId;     // 카카오 플레이스 ID
    private Double latitude;    // 위도
    private Double longitude;   // 경도
    private String address;     // 주소
    private String roadAddress; // 도로명 주소
    private String phone;       // 전화번호
    private String placeUrl;    // 카카오 플레이스 URL

    @OneToMany(mappedBy = "winner", cascade = CascadeType.ALL)
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant1", cascade = CascadeType.ALL)
    private List<Match> matches1 = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant2", cascade = CascadeType.ALL)
    private List<Match> matches2 = new ArrayList<>();

    @Builder
    public Restaurant(
            String name,
            String category,
            Integer distance,
            String imageUrl,
            String kakaoId,
            Double latitude,
            Double longitude,
            String address,
            String roadAddress,
            String phone,
            String placeUrl
    ) {
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.imageUrl = imageUrl;
        this.kakaoId = kakaoId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.winCount = 0;
        this.playCount = 0;
    }

    // 기존 더미데이터용 Builder
    public static class DummyBuilder {
        public static Restaurant buildDummy(
                String name,
                String category,
                Integer distance,
                String imageUrl
        ) {
            return Restaurant.builder()
                    .name(name)
                    .category(category)
                    .distance(distance)
                    .imageUrl(imageUrl)
                    .build();
        }
    }

    public void incrementWinCount() {
        this.winCount++;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }
}
