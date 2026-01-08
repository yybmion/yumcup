package mioneF.yumCup.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Google Places API 클라이언트 구현체
 */
@Component
public class GooglePlacesApiClient extends AbstractWebClientApiClient {

	private final String apiKey;

	private static final int MAX_PHOTO_WIDTH = 400;
	private static final String PLACE_FIELDS =
			"place_id,name,rating,user_ratings_total,photos,price_level,opening_hours/open_now";

	public GooglePlacesApiClient(
			@Qualifier("googleWebClient") WebClient webClient,
			ObjectMapper objectMapper,
			@Value("${google.api.key}") String apiKey) {
		super( webClient, objectMapper );
		this.apiKey = apiKey;
	}

	/**
	 * Google Places API - Find Place 요청
	 *
	 * <p>주어진 이름과 위치로 장소를 검색합니다.</p>
	 *
	 * @param name 장소 이름 (예: "스타벅스 강남점")
	 * @param lat 위도
	 * @param lng 경도
	 * @param responseType 응답 타입 클래스
	 * @param <T> 응답 타입
	 *
	 * @return 파싱된 응답 객체
	 */
	public <T> T findPlace(String name, double lat, double lng, Class<T> responseType) {
		Map<String, String> params = buildFindPlaceParams( name, lat, lng );
		return get( "/maps/api/place/findplacefromtext/json", params, responseType );
	}

	/**
	 * Find Place API 쿼리 파라미터 생성
	 *
	 * @param name 장소 이름
	 * @param lat 위도
	 * @param lng 경도
	 *
	 * @return 쿼리 파라미터 Map
	 */
	private Map<String, String> buildFindPlaceParams(String name, double lat, double lng) {
		Map<String, String> params = new HashMap<>();
		params.put( "input", name );
		params.put( "inputtype", "textquery" );
		params.put( "locationbias", String.format( "circle:100@%f,%f", lat, lng ) );
		params.put( "fields", PLACE_FIELDS );
		params.put( "key", apiKey );
		return params;
	}

	/**
	 * Google Places Photo URL 생성
	 *
	 * <p>Photo Reference를 실제 이미지 URL로 변환합니다.</p>
	 *
	 * @param photoReference Google Places API에서 받은 photo_reference
	 *
	 * @return 이미지 URL (최대 너비 400px)
	 */
	public String generatePhotoUrl(String photoReference) {
		if ( photoReference == null || photoReference.isEmpty() ) {
			return null;
		}

		return String.format(
				"https://maps.googleapis.com/maps/api/place/photo?maxwidth=%d&photo_reference=%s&key=%s",
				MAX_PHOTO_WIDTH,
				photoReference,
				apiKey
		);
	}

	@Override
	protected String getApiName() {
		return "Google Places API";
	}
}
