package mioneF.yumCup.repository;

import java.util.Optional;
import mioneF.yumCup.domain.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByKakaoId(String kakaoId);
}
