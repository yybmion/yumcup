package mioneF.yumCup.repository;

import mioneF.yumCup.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
