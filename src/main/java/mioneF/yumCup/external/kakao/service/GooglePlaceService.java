package mioneF.yumCup.external.kakao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.infrastructure.api.GooglePlacesApiClient;
import mioneF.yumCup.performance.Monitored;
import org.springframework.stereotype.Service;

/**
 * Google Places API 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GooglePlaceService {

	private final GooglePlacesApiClient googleApiClient;

	/**
	 * Google Places API로 장소 정보 조회
	 */
	@Monitored
	public GooglePlaceResponse findPlace(String kakaoId, String name, double lat, double lng) {
		log.info( "Fetching place info from Google API for: {} (kakaoId: {})", name, kakaoId );

		GooglePlaceResponse response = googleApiClient.findPlace( name, lat, lng, GooglePlaceResponse.class );

		log.info(
				"Google API response for {}: status={}, candidates={}",
				name,
				response.status(),
				response.candidates() != null ? response.candidates().size() : 0
		);

		return response;
	}

	/**
	 * 사진 URL 생성
	 */
	public String getPhotoUrl(String photoReference) {
		if ( photoReference == null ) {
			log.warn( "Received null photo reference" );
			return null;
		}

		log.debug( "Generating photo URL for reference: {}", photoReference );
		return googleApiClient.generatePhotoUrl( photoReference );
	}
}
