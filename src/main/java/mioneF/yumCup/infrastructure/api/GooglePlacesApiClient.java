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
	 */
	public <T> T findPlace(String name, double lat, double lng, Class<T> responseType) {
		Map<String, String> params = buildFindPlaceParams( name, lat, lng );
		return get( "/maps/api/place/findplacefromtext/json", params, responseType );
	}

	/**
	 * Find Place API 쿼리 파라미터 생성
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
