package mioneF.yumCup.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
