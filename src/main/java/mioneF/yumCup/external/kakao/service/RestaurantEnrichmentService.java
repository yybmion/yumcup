package mioneF.yumCup.external.kakao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.domain.entity.Restaurant;
import mioneF.yumCup.external.kakao.dto.KakaoDocument;
import org.springframework.stereotype.Service;

/**
 * 레스토랑 정보 보강 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantEnrichmentService {

	private final GooglePlaceService googlePlaceService;

	/**
	 * Kakao 문서를 Restaurant로 변환하고 Google 정보로 보강
	 */
	public Restaurant enrichWithGoogleInfo(KakaoDocument doc) {
		Restaurant restaurant = createBaseRestaurant( doc );

		try {
			GooglePlaceResponse googleResponse = googlePlaceService.findPlace(
					doc.id(),
					doc.place_name(),
					Double.parseDouble( doc.y() ),
					Double.parseDouble( doc.x() )
			);
			if ( isValidGoogleResponse( googleResponse ) ) {
				GooglePlaceResponse.GooglePlace place = googleResponse.candidates().get( 0 );
				restaurant = enrichWithGooglePlace( restaurant, place );
				log.debug( "Successfully enriched {} with Google info", doc.place_name() );
			}
			else {
				log.debug( "No Google info available for {}", doc.place_name() );
			}

		}
		catch (Exception e) {
			log.warn(
					"Failed to enrich {} with Google info: {}",
					doc.place_name(), e.getMessage()
			);
		}

		return restaurant;
	}

	/**
	 * Kakao 문서로 기본 Restaurant 엔티티 생성
	 */
	private Restaurant createBaseRestaurant(KakaoDocument doc) {
		String category = extractMainCategory( doc.category_name() );

		return Restaurant.builder()
				.name( doc.place_name() )
				.category( category )
				.distance( Integer.parseInt( doc.distance() ) )
				.kakaoId( doc.id() )
				.latitude( Double.parseDouble( doc.y() ) )
				.longitude( Double.parseDouble( doc.x() ) )
				.address( doc.address_name() )
				.roadAddress( doc.road_address_name() )
				.phone( doc.phone() )
				.placeUrl( doc.place_url() )
				.build();
	}

	/**
	 * Google Place 정보로 Restaurant 보강
	 */
	private Restaurant enrichWithGooglePlace(
			Restaurant restaurant,
			GooglePlaceResponse.GooglePlace place) {

		String photoUrl = extractPhotoUrl( place );
		Boolean isOpenNow = extractOpeningStatus( place );

		return restaurant.toBuilder()
				.rating( place.rating() )
				.ratingCount( place.user_ratings_total() )
				.photoUrl( photoUrl )
				.priceLevel( place.price_level() )
				.isOpenNow( isOpenNow )
				.build();
	}

	/**
	 * Google Place에서 사진 URL 추출
	 */
	private String extractPhotoUrl(GooglePlaceResponse.GooglePlace place) {
		if ( place.photos() != null && !place.photos().isEmpty() ) {
			String photoReference = place.photos().get( 0 ).photo_reference();
			return googlePlaceService.getPhotoUrl( photoReference );
		}
		return null;
	}

	/**
	 * Google Place에서 영업 상태 추출
	 */
	private Boolean extractOpeningStatus(GooglePlaceResponse.GooglePlace place) {
		if ( place.opening_hours() != null ) {
			return place.opening_hours().open_now();
		}
		return null;
	}

	/**
	 * Google API 응답 유효성 검증
	 */
	private boolean isValidGoogleResponse(GooglePlaceResponse response) {
		return response != null
				&& response.candidates() != null
				&& !response.candidates().isEmpty();
	}

	/**
	 * 카테고리 이름에서 메인 카테고리 추출
	 */
	private String extractMainCategory(String categoryName) {
		String[] categories = categoryName.split( " > " );
		return categories.length > 1 ? categories[1] : categories[0];
	}
}
