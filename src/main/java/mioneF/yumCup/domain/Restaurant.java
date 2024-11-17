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

    @OneToMany(mappedBy = "winner", cascade = CascadeType.ALL)
    private List<Game> games = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant1", cascade = CascadeType.ALL)
    private List<Match> matches1 = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant2", cascade = CascadeType.ALL)
    private List<Match> matches2 = new ArrayList<>();

    @Builder
    public Restaurant(String name, String category, Integer distance, String imageUrl) {
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.imageUrl = imageUrl;
        this.winCount = 0;
        this.playCount = 0;
    }

    public void incrementWinCount() {
        this.winCount++;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }
}
