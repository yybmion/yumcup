package mioneF.yumCup.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import mioneF.yumCup.domain.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Restaurant> findByKakaoId(String kakaoId);
}
