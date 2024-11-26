package mioneF.yumCup.repository;

import mioneF.yumCup.domain.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
