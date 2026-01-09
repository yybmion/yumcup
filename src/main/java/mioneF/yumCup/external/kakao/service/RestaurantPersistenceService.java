package mioneF.yumCup.external.kakao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 레스토랑 영속성 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantPersistenceService {

	private final RestaurantRepository restaurantRepository;

	/**
	 * 레스토랑 목록을 저장하거나 업데이트
	 */
	@Transactional
	public List<Restaurant> saveOrUpdate(List<Restaurant> restaurants) {
		if ( restaurants == null || restaurants.isEmpty() ) {
			log.debug( "No restaurants to save" );
			return List.of();
		}

		log.debug( "Processing {} restaurants for save/update", restaurants.size() );

		List<String> kakaoIds = restaurants.stream()
				.map( Restaurant::getKakaoId )
				.collect( Collectors.toList() );

		List<Restaurant> existingRestaurants = restaurantRepository.findByKakaoIdIn( kakaoIds );

		Map<String, Restaurant> existingMap = existingRestaurants.stream()
				.collect( Collectors.toMap( Restaurant::getKakaoId, Function.identity() ) );

		List<Restaurant> newRestaurants = new ArrayList<>();
		List<Restaurant> restaurantsToUpdate = new ArrayList<>();
		List<Restaurant> result = new ArrayList<>();

		for ( Restaurant restaurant : restaurants ) {
			Restaurant existing = existingMap.get( restaurant.getKakaoId() );

			if ( existing == null ) {
				newRestaurants.add( restaurant );
			}
			else {
				if ( shouldUpdate( existing ) ) {
					existing.updateWithNewInfo( restaurant );
					restaurantsToUpdate.add( existing );
				}
				result.add( existing );
			}
		}

		if ( !newRestaurants.isEmpty() ) {
			log.info( "Batch inserting {} new restaurants", newRestaurants.size() );
			List<Restaurant> savedRestaurants = restaurantRepository.saveAll( newRestaurants );
			result.addAll( savedRestaurants );
		}

		if ( !restaurantsToUpdate.isEmpty() ) {
			log.info( "Batch updating {} restaurants", restaurantsToUpdate.size() );
			restaurantRepository.saveAll( restaurantsToUpdate );
		}

		log.debug(
				"Completed processing: {} new, {} updated, {} total",
				newRestaurants.size(),
				restaurantsToUpdate.size(),
				result.size()
		);

		return result;
	}

	/**
	 * 레스토랑 업데이트 필요 여부 확인
	 */
	private boolean shouldUpdate(Restaurant restaurant) {
		LocalDateTime lastUpdate = restaurant.getUpdatedAt();
		LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays( 14 );

		return lastUpdate.isBefore( fourteenDaysAgo );
	}

	/**
	 * 특정 Kakao ID 목록으로 레스토랑 조회
	 */
	@Transactional(readOnly = true)
	public List<Restaurant> findByKakaoIds(List<String> kakaoIds) {
		if ( kakaoIds == null || kakaoIds.isEmpty() ) {
			return List.of();
		}

		return restaurantRepository.findByKakaoIdIn( kakaoIds );
	}
}
