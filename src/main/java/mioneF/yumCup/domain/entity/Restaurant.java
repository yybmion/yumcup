package mioneF.yumCup.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private Integer distance;
    private String imageUrl;
    private Integer winCount;
    private Integer playCount;

    // 카카오맵 연동을 위한 필드들
    private String kakaoId;
    private Double latitude;
    private Double longitude;
    private String address;
    private String roadAddress;
    private String phone;
    private String placeUrl;

    @OneToMany(mappedBy = "winner", cascade = CascadeType.ALL)
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant1", cascade = CascadeType.ALL)
    private List<Match> matches1 = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant2", cascade = CascadeType.ALL)
    private List<Match> matches2 = new ArrayList<>();

    @Builder(toBuilder = true)
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

    // 더미데이터용 정적 팩토리 메서드
    public static Restaurant createDummy(String name, String category, Integer distance, String imageUrl) {
        return Restaurant.builder()
                .name(name)
                .category(category)
                .distance(distance)
                .imageUrl(imageUrl)
                .build();
    }

    public void incrementWinCount() {
        this.winCount++;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }

    public void updateDistance(Integer newDistance) {
        this.distance = newDistance;
    }
}
