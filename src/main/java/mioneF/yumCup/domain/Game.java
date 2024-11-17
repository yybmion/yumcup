package mioneF.yumCup.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer totalRounds;  // 총 라운드 수 (16강은 16, 8강은 8 등)

    @ManyToOne(fetch = FetchType.LAZY)
    private Restaurant winner;    // 최종 우승 음식점

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Match> matches = new ArrayList<>();

    @Builder
    public Game(Integer totalRounds) {
        this.totalRounds = totalRounds;
        this.startedAt = LocalDateTime.now();
    }

    public void complete(Restaurant winner) {
        this.winner = winner;
        this.endedAt = LocalDateTime.now();
    }

    public void addMatch(Match match) {
        this.matches.add(match);
        match.setGame(this);
    }
}
