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
	 * 메모리 최적화: 중간 컬렉션 최소화, 초기 용량 지정으로 재할당 방지
	 */
	@Transactional
	public List<Restaurant> saveOrUpdate(List<Restaurant> restaurants) {
		if ( restaurants == null || restaurants.isEmpty() ) {
			log.debug( "No restaurants to save" );
			return List.of();
		}

		int size = restaurants.size();
		log.debug( "Processing {} restaurants for save/update", size );

		// 1. kakaoIds 추출 (DB 쿼리용) - toList()로 불변 리스트 생성
		List<String> kakaoIds = restaurants.stream()
				.map( Restaurant::getKakaoId )
				.toList();

		// 2. 기존 레스토랑 조회 및 맵 생성 (단일 스트림으로 처리)
		Map<String, Restaurant> existingMap = restaurantRepository.findByKakaoIdIn( kakaoIds )
				.stream()
				.collect( Collectors.toMap( Restaurant::getKakaoId, Function.identity() ) );

		// 3. 초기 용량 지정으로 ArrayList 재할당 방지 (중간 리스트 3개 → 2개로 축소)
		List<Restaurant> result = new ArrayList<>( size );
		List<Restaurant> newRestaurants = new ArrayList<>( size - existingMap.size() );
		int updateCount = 0;

		// 4. 단일 패스로 분류 (restaurantsToUpdate 리스트 제거)
		for ( Restaurant restaurant : restaurants ) {
			Restaurant existing = existingMap.get( restaurant.getKakaoId() );

			if ( existing == null ) {
				newRestaurants.add( restaurant );
			}
			else {
				if ( shouldUpdate( existing ) ) {
					existing.updateWithNewInfo( restaurant );
					updateCount++;
				}
				result.add( existing );
			}
		}

		// 5. 새 레스토랑 배치 저장
		if ( !newRestaurants.isEmpty() ) {
			log.info( "Batch inserting {} new restaurants", newRestaurants.size() );
			result.addAll( restaurantRepository.saveAll( newRestaurants ) );
		}

		// 업데이트된 레스토랑은 @Transactional dirty checking으로 자동 flush
		if ( updateCount > 0 ) {
			log.info( "Updated {} restaurants via dirty checking", updateCount );
		}

		log.debug(
				"Completed processing: {} new, {} updated, {} total",
				newRestaurants.size(), updateCount, result.size()
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
