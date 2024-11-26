package mioneF.yumCup.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant1;  // 첫 번째 음식점

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant restaurant2;  // 두 번째 음식점

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant winner;      // 승자

    private Integer round;         // 현재 라운드 (16, 8, 4, 2)
    private Integer matchOrder;    // 해당 라운드에서의 순서 (1, 2, 3...)

    private LocalDateTime createdAt;

    @Builder
    public Match(Restaurant restaurant1, Restaurant restaurant2, Integer round, Integer matchOrder) {
        this.restaurant1 = restaurant1;
        this.restaurant2 = restaurant2;
        this.round = round;
        this.matchOrder = matchOrder;
        this.createdAt = LocalDateTime.now();
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setWinner(Restaurant winner) {
        this.winner = winner;
        winner.incrementPlayCount();

        // 다른 참가 음식점의 플레이 카운트도 증가
        Restaurant loser = (winner.equals(restaurant1)) ? restaurant2 : restaurant1;
        loser.incrementPlayCount();
    }
}