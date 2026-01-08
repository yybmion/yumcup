package mioneF.yumCup.infrastructure.api;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import mioneF.yumCup.domain.dto.response.GooglePlaceResponse;
import mioneF.yumCup.exception.ExternalApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GooglePlacesApiClient 단위 테스트")
class GooglePlacesApiClientTest {

	private MockWebServer mockWebServer;
	private GooglePlacesApiClient googleApiClient;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		WebClient webClient = WebClient.builder()
				.baseUrl( mockWebServer.url( "/" ).toString() )
				.build();

		objectMapper = new ObjectMapper();

		googleApiClient = new GooglePlacesApiClient( webClient, objectMapper, "test-api-key" );
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	@DisplayName("findPlace() - 성공 케이스: 정상 응답을 파싱한다")
	void findPlace_Success() throws Exception {
		String mockResponseJson = """
				{
				    "candidates": [
				        {
				            "place_id": "ChIJ123",
				            "name": "Test Restaurant",
				            "rating": 4.5,
				            "user_ratings_total": 100
				        }
				    ],
				    "status": "OK"
				}
				""";

		mockWebServer.enqueue( new MockResponse()
									   .setBody( mockResponseJson )
									   .addHeader( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE )
									   .setResponseCode( 200 ) );

		// When: API 호출
		GooglePlaceResponse response = googleApiClient.findPlace(
				"Test Restaurant",
				37.5665,
				126.9780,
				GooglePlaceResponse.class
		);

		// Then: 응답 검증
		assertThat( response ).isNotNull();
		assertThat( response.status() ).isEqualTo( "OK" );
		assertThat( response.candidates() ).hasSize( 1 );
		assertThat( response.candidates().get( 0 ).name() ).isEqualTo( "Test Restaurant" );
		assertThat( response.candidates().get( 0 ).rating() ).isEqualTo( 4.5 );

		// 요청 검증
		RecordedRequest recordedRequest = mockWebServer.takeRequest();
		assertThat( recordedRequest.getPath() ).contains( "/maps/api/place/findplacefromtext/json" );
		assertThat( recordedRequest.getPath() ).contains( "input=Test+Restaurant" );
		assertThat( recordedRequest.getPath() ).contains( "key=test-api-key" );
	}

	@Test
	@DisplayName("findPlace() - 빈 결과: candidates가 비어있어도 정상 처리")
	void findPlace_EmptyCandidates() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "candidates": [],
				    "status": "ZERO_RESULTS"
				}
				""";

		mockWebServer.enqueue( new MockResponse()
									   .setBody( mockResponseJson )
									   .setResponseCode( 200 ) );

		// When
		GooglePlaceResponse response = googleApiClient.findPlace(
				"Nonexistent Place",
				37.5665,
				126.9780,
				GooglePlaceResponse.class
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.status() ).isEqualTo( "ZERO_RESULTS" );
		assertThat( response.candidates() ).isEmpty();
	}

	@Test
	@DisplayName("findPlace() - 재시도 성공: 1차 실패 후 2차에서 성공")
	void findPlace_RetrySuccess() throws Exception {
		// Given: 첫 번째 요청은 실패, 두 번째 요청은 성공
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse()
									   .setBody( """
														 {
														     "candidates": [{"place_id": "ChIJ123", "name": "Test"}],
														     "status": "OK"
														 }
														 """ )
									   .setResponseCode( 200 ) );

		// When
		GooglePlaceResponse response = googleApiClient.findPlace(
				"Test",
				37.5665,
				126.9780,
				GooglePlaceResponse.class
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.status() ).isEqualTo( "OK" );

		// 2번 요청되었는지 확인
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName("findPlace() - 재시도 실패: 3번 모두 실패하면 예외 발생")
	void findPlace_RetryFailure() {
		// Given: 3번 모두 실패 응답
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );

		// When & Then
		assertThatThrownBy( () ->
									googleApiClient.findPlace( "Test", 37.5665, 126.9780, GooglePlaceResponse.class )
		)
				.isInstanceOf( ExternalApiException.class )
				.hasMessageContaining( "failed after 3 attempts" );

		// 3번 재시도 확인
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName("findPlace() - API 에러 응답: 적절한 예외로 변환")
	void findPlace_ApiError() {
		// Given
		mockWebServer.enqueue( new MockResponse()
									   .setResponseCode( 400 )
									   .setBody( """
														 {
														     "error_message": "Invalid request",
														     "status": "INVALID_REQUEST"
														 }
														 """ ) );

		// When & Then
		assertThatThrownBy( () ->
									googleApiClient.findPlace( "", 37.5665, 126.9780, GooglePlaceResponse.class )
		)
				.isInstanceOf( ExternalApiException.class )
				.hasMessageContaining( "API call failed" );
	}

	@Test
	@DisplayName("generatePhotoUrl() - 정상: 올바른 URL 생성")
	void generatePhotoUrl_Success() {
		// When
		String photoUrl = googleApiClient.generatePhotoUrl( "test-photo-ref" );

		// Then
		assertThat( photoUrl ).isNotNull();
		assertThat( photoUrl ).contains( "https://maps.googleapis.com/maps/api/place/photo" );
		assertThat( photoUrl ).contains( "photo_reference=test-photo-ref" );
		assertThat( photoUrl ).contains( "key=test-api-key" );
		assertThat( photoUrl ).contains( "maxwidth=400" );
	}

	@Test
	@DisplayName("generatePhotoUrl() - null 입력: null 반환")
	void generatePhotoUrl_NullInput() {
		// When
		String photoUrl = googleApiClient.generatePhotoUrl( null );

		// Then
		assertThat( photoUrl ).isNull();
	}

	@Test
	@DisplayName("generatePhotoUrl() - 빈 문자열: null 반환")
	void generatePhotoUrl_EmptyInput() {
		// When
		String photoUrl = googleApiClient.generatePhotoUrl( "" );

		// Then
		assertThat( photoUrl ).isNull();
	}

	@Test
	@DisplayName("getApiName() - API 이름 반환")
	void getApiName() {
		// When
		String apiName = googleApiClient.getApiName();

		// Then
		assertThat( apiName ).isEqualTo( "Google Places API" );
	}
}
