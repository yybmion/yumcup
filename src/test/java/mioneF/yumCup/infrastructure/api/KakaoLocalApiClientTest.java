package mioneF.yumCup.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import mioneF.yumCup.external.kakao.dto.KakaoSearchResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * KakaoLocalApiClient 단위 테스트
 */
@DisplayName("KakaoLocalApiClient 단위 테스트")
class KakaoLocalApiClientTest {

	private MockWebServer mockWebServer;
	private KakaoLocalApiClient kakaoApiClient;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		WebClient webClient = WebClient.builder()
				.baseUrl( mockWebServer.url( "/" ).toString() )
				.defaultHeader( "Authorization", "KakaoAK test-key" )
				.build();

		kakaoApiClient = new KakaoLocalApiClient( webClient, new ObjectMapper() );
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	@DisplayName("searchByCategory() - 성공: 음식점 목록 반환")
	void searchByCategory_Success() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "meta": {
				        "total_count": 2,
				        "pageable_count": 2,
				        "is_end": false
				    },
				    "documents": [
				        {
				            "id": "12345",
				            "place_name": "Test Restaurant 1",
				            "category_name": "음식점 > 한식",
				            "phone": "02-1234-5678",
				            "address_name": "서울 강남구",
				            "road_address_name": "서울 강남구 테헤란로",
				            "x": "127.0276",
				            "y": "37.4979",
				            "place_url": "http://place.map.kakao.com/12345",
				            "distance": "100"
				        },
				        {
				            "id": "67890",
				            "place_name": "Test Restaurant 2",
				            "category_name": "음식점 > 중식",
				            "phone": "02-9876-5432",
				            "address_name": "서울 강남구",
				            "road_address_name": "서울 강남구 선릉로",
				            "x": "127.0486",
				            "y": "37.5044",
				            "place_url": "http://place.map.kakao.com/67890",
				            "distance": "200"
				        }
				    ]
				}
				""";

		mockWebServer.enqueue( new MockResponse()
									   .setBody( mockResponseJson )
									   .setResponseCode( 200 ) );

		// When
		KakaoSearchResponse response = kakaoApiClient.searchByCategory(
				37.5665,
				126.9780,
				1000,
				1,
				KakaoSearchResponse.class
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.meta().total_count() ).isEqualTo( 2 );
		assertThat( response.documents() ).hasSize( 2 );
		assertThat( response.documents().get( 0 ).place_name() ).isEqualTo( "Test Restaurant 1" );
		assertThat( response.documents().get( 1 ).place_name() ).isEqualTo( "Test Restaurant 2" );

		// 요청 검증
		RecordedRequest recordedRequest = mockWebServer.takeRequest();
		String path = recordedRequest.getPath();

		assertThat( path ).contains( "/v2/local/search/category.json" );
		assertThat( path ).contains( "category_group_code=FD6" );
		assertThat( path ).contains( "x=126.978" );  // 경도
		assertThat( path ).contains( "y=37.5665" );  // 위도
		assertThat( path ).contains( "radius=1000" );
		assertThat( path ).contains( "size=15" );
		assertThat( path ).contains( "page=1" );
	}

	@Test
	@DisplayName("searchByCategory() - 빈 결과: documents가 비어있어도 정상 처리")
	void searchByCategory_EmptyResult() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "meta": {
				        "total_count": 0,
				        "pageable_count": 0,
				        "is_end": true
				    },
				    "documents": []
				}
				""";

		mockWebServer.enqueue( new MockResponse()
									   .setBody( mockResponseJson )
									   .setResponseCode( 200 ) );

		// When
		KakaoSearchResponse response = kakaoApiClient.searchByCategory(
				37.5665,
				126.9780,
				1000,
				1,
				KakaoSearchResponse.class
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.meta().total_count() ).isEqualTo( 0 );
		assertThat( response.documents() ).isEmpty();
	}

	@Test
	@DisplayName("searchByCategory() - 페이징: 여러 페이지 요청 가능")
	void searchByCategory_Pagination() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "meta": {
				        "total_count": 45,
				        "pageable_count": 45,
				        "is_end": false
				    },
				    "documents": [
				        {"id": "1", "place_name": "Restaurant 1", "x": "127.0", "y": "37.5", "distance": "100"}
				    ]
				}
				""";

		mockWebServer.enqueue( new MockResponse().setBody( mockResponseJson ).setResponseCode( 200 ) );

		// When: 2페이지 요청
		kakaoApiClient.searchByCategory( 37.5665, 126.9780, 1000, 2, KakaoSearchResponse.class );

		// Then
		RecordedRequest request = mockWebServer.takeRequest();
		assertThat( request.getPath() ).contains( "page=2" );
	}

	@Test
	@DisplayName("searchByCategory() - 재시도: 실패 후 재시도 성공")
	void searchByCategory_RetrySuccess() throws Exception {
		// Given
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse()
									   .setBody( """
														 {
														     "meta": {"total_count": 1, "pageable_count": 1, "is_end": true},
														     "documents": [{"id": "1", "place_name": "Test", "x": "127.0", "y": "37.5", "distance": "100"}]
														 }
														 """ )
									   .setResponseCode( 200 ) );

		// When
		KakaoSearchResponse response = kakaoApiClient.searchByCategory(
				37.5665,
				126.9780,
				1000,
				1,
				KakaoSearchResponse.class
		);

		// Then
		assertThat( response ).isNotNull();
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName("searchByCategory() - API 에러: 예외 발생")
	void searchByCategory_ApiError() {
		// Given
		mockWebServer.enqueue( new MockResponse()
									   .setResponseCode( 401 )
									   .setBody( """
														 {
														     "errorType": "InvalidApiKey",
														     "message": "Invalid API Key"
														 }
														 """ ) );

		// When & Then
		assertThatThrownBy( () ->
									kakaoApiClient.searchByCategory(
											37.5665,
											126.9780,
											1000,
											1,
											KakaoSearchResponse.class
									)
		).hasMessageContaining( "API call failed" );
	}

	@Test
	@DisplayName("searchByCategory() - 다양한 반경: 쿼리 파라미터 정확히 전달")
	void searchByCategory_DifferentRadius() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "meta": {"total_count": 0, "pageable_count": 0, "is_end": true},
				    "documents": []
				}
				""";

		mockWebServer.enqueue( new MockResponse().setBody( mockResponseJson ).setResponseCode( 200 ) );

		// When: 5000m 반경
		kakaoApiClient.searchByCategory( 37.5665, 126.9780, 5000, 1, KakaoSearchResponse.class );

		// Then
		RecordedRequest request = mockWebServer.takeRequest();
		assertThat( request.getPath() ).contains( "radius=5000" );
	}

	@Test
	@DisplayName("getApiName() - API 이름 반환")
	void getApiName() {
		// When
		String apiName = kakaoApiClient.getApiName();

		// Then
		assertThat( apiName ).isEqualTo( "Kakao Local API" );
	}
}
