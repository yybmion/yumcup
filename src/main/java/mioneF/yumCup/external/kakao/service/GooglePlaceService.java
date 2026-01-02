package mioneF.yumCup.external.kakao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.performance.Monitored;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class GooglePlaceService {
	private final WebClient googleWebClient;
	private final String googleApiKey;

	private static final int MAX_PHOTO_WIDTH = 400;
	private static final String PHOTO_URL_FORMAT =
			"https://maps.googleapis.com/maps/api/place/photo?maxwidth=%d&photo_reference=%s&key=%s";

	public GooglePlaceService(
			@Qualifier("googleWebClient") WebClient googleWebClient,
			@Value("${google.api.key}") String googleApiKey) {
		this.googleWebClient = googleWebClient;
		this.googleApiKey = googleApiKey;
	}

	@Monitored
	public GooglePlaceResponse findPlace(String kakaoId, String name, double lat, double lng) throws Exception {
		log.info( "Fetching place info from Google API for: {} (kakaoId: {})", name, kakaoId );
		return fetchFromGoogleApi( name, lat, lng );
	}

	private GooglePlaceResponse fetchFromGoogleApi(String name, double lat, double lng) throws Exception {
		String response = googleWebClient.get()
				.uri( uriBuilder -> uriBuilder
						.path( "/maps/api/place/findplacefromtext/json" )
						.queryParam( "input", name )
						.queryParam( "inputtype", "textquery" )
						.queryParam( "locationbias", String.format( "circle:100@%f,%f", lat, lng ) )
						.queryParam(
								"fields",
								"place_id,name,rating,user_ratings_total,photos,price_level,opening_hours/open_now"
						)
						.queryParam( "key", googleApiKey )
						.build() )
				.retrieve()
				.bodyToMono( String.class )
				.block();

		GooglePlaceResponse googleResponse = new ObjectMapper().readValue( response, GooglePlaceResponse.class );
		log.info(
				"Google API response for {}: status={}, candidates={}",
				name,
				googleResponse.status(),
				googleResponse.candidates() != null ? googleResponse.candidates().size() : 0
		);
		return googleResponse;
	}

	public String getPhotoUrl(String photoReference) {
		if ( photoReference == null ) {
			log.warn( "Received null photo reference" );
			return null;
		}

		log.debug( "Generating photo URL for reference: {}", photoReference );
		return generatePhotoUrl( photoReference );
	}

	public String generatePhotoUrl(String photoReference) {
		return String.format( PHOTO_URL_FORMAT, MAX_PHOTO_WIDTH, photoReference, googleApiKey );
	}
}
