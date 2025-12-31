package mioneF.yumCup.repository;

import java.util.List;

import mioneF.yumCup.domain.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	@Query("SELECT r FROM Restaurant r WHERE r.kakaoId IN :kakaoIds")
	List<Restaurant> findByKakaoIdIn(@Param("kakaoIds") List<String> kakaoIds);

}
