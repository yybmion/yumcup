package mioneF.yumCup.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import mioneF.yumCup.exception.ExternalApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * AbstractWebClientApiClient 공통 로직 테스트
 */
@DisplayName("AbstractWebClientApiClient 공통 로직 테스트")
class AbstractWebClientApiClientTest {

	private MockWebServer mockWebServer;
	private TestApiClient testApiClient;

	/**
	 * 테스트용 API 클라이언트 구현체
	 */
	static class TestApiClient extends AbstractWebClientApiClient {
		public TestApiClient(WebClient webClient, ObjectMapper objectMapper) {
			super( webClient, objectMapper );
		}

		@Override
		protected String getApiName() {
			return "Test API";
		}
	}

	/**
	 * 테스트용 응답 DTO
	 */
	record TestResponse(String message, int code) {
	}

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();

		WebClient webClient = WebClient.builder()
				.baseUrl( mockWebServer.url( "/" ).toString() )
				.build();

		testApiClient = new TestApiClient( webClient, new ObjectMapper() );
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	@DisplayName("get() - 정상 케이스: 성공 응답 파싱")
	void get_Success() throws Exception {
		// Given
		String mockResponseJson = """
				{
				    "message": "success",
				    "code": 200
				}
				""";

		mockWebServer.enqueue( new MockResponse()
									   .setBody( mockResponseJson )
									   .setResponseCode( 200 ) );

		// When
		TestResponse response = testApiClient.get( "/test", TestResponse.class );

		// Then
		assertThat( response ).isNotNull();
		assertThat( response.message() ).isEqualTo( "success" );
		assertThat( response.code() ).isEqualTo( 200 );
	}

	@Test
	@DisplayName("get() - 쿼리 파라미터: 올바른 URL 생성")
	void get_WithQueryParams() throws Exception {
		// Given
		Map<String, String> params = new HashMap<>();
		params.put( "key1", "value1" );
		params.put( "key2", "value2" );

		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"ok\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		testApiClient.get( "/test", params, TestResponse.class );

		// Then
		String requestPath = mockWebServer.takeRequest().getPath();
		assertThat( requestPath ).contains( "/test?" );
		assertThat( requestPath ).contains( "key1=value1" );
		assertThat( requestPath ).contains( "key2=value2" );
	}

	@Test
	@DisplayName("재시도 - 1차 실패 후 2차 성공")
	void retry_FirstFailThenSuccess() throws Exception {
		// Given
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"success\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		long startTime = System.currentTimeMillis();
		TestResponse response = testApiClient.get( "/test", TestResponse.class );
		long duration = System.currentTimeMillis() - startTime;

		// Then
		assertThat( response.message() ).isEqualTo( "success" );
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 2 );

		// 재시도 지연 확인 (최소 1초)
		assertThat( duration ).isGreaterThanOrEqualTo( 1000 );
	}

	@Test
	@DisplayName("재시도 - 2차 실패 후 3차 성공")
	void retry_TwoFailsThenSuccess() throws Exception {
		// Given
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"success\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		long startTime = System.currentTimeMillis();
		TestResponse response = testApiClient.get( "/test", TestResponse.class );
		long duration = System.currentTimeMillis() - startTime;

		// Then
		assertThat( response.message() ).isEqualTo( "success" );
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 3 );

		// 재시도 지연 확인 (1초 + 2초 = 최소 3초)
		assertThat( duration ).isGreaterThanOrEqualTo( 3000 );
	}

	@Test
	@DisplayName("재시도 - 3번 모두 실패: ExternalApiException 발생")
	void retry_AllFailures() {
		// Given
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );

		// When & Then
		assertThatThrownBy( () -> testApiClient.get( "/test", TestResponse.class ) )
				.isInstanceOf( ExternalApiException.class )
				.hasMessageContaining( "failed after 3 attempts" );

		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName("에러 처리 - 4xx 에러: ExternalApiException으로 변환")
	void errorHandling_4xxError() {
		// Given
		mockWebServer.enqueue( new MockResponse()
									   .setResponseCode( 400 )
									   .setBody( "{\"error\":\"Bad Request\"}" ) );

		// When & Then
		assertThatThrownBy( () -> testApiClient.get( "/test", TestResponse.class ) )
				.isInstanceOf( ExternalApiException.class )
				.hasMessageContaining( "Test API" )
				.hasMessageContaining( "API call failed" );
	}

	@Test
	@DisplayName("에러 처리 - 5xx 에러: 재시도 후 예외 발생")
	void errorHandling_5xxError() {
		// Given: 3번 모두 500 에러
		mockWebServer.enqueue( new MockResponse().setResponseCode( 503 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 503 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 503 ) );

		// When & Then
		assertThatThrownBy( () -> testApiClient.get( "/test", TestResponse.class ) )
				.isInstanceOf( ExternalApiException.class );

		// 재시도 확인
		assertThat( mockWebServer.getRequestCount() ).isEqualTo( 3 );
	}

	@Test
	@DisplayName("타임아웃 - 응답 지연: 타임아웃 에러")
	void timeout_SlowResponse() {
		// Given: 15초 지연 (타임아웃은 10초)
		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"slow\",\"code\":200}" )
									   .setBodyDelay( 15, TimeUnit.SECONDS ) );

		// When & Then
		assertThatThrownBy( () -> testApiClient.get( "/test", TestResponse.class ) )
				.isInstanceOf( ExternalApiException.class );
	}

	@Test
	@DisplayName("post() - 정상 케이스: 요청 바디와 함께 POST 요청")
	void post_Success() throws Exception {
		// Given
		TestResponse requestBody = new TestResponse( "request", 100 );

		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"response\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		TestResponse response = testApiClient.post( "/test", requestBody, TestResponse.class );

		// Then
		assertThat( response.message() ).isEqualTo( "response" );

		// 요청 바디 검증
		String requestBodyStr = mockWebServer.takeRequest().getBody().readUtf8();
		assertThat( requestBodyStr ).contains( "request" );
		assertThat( requestBodyStr ).contains( "100" );
	}

	@Test
	@DisplayName("URL 빌드 - 기존 쿼리 파라미터 있음: & 로 연결")
	void buildUrl_ExistingQueryParam() throws Exception {
		// Given
		Map<String, String> params = new HashMap<>();
		params.put( "newKey", "newValue" );

		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"ok\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		testApiClient.get( "/test?existing=value", params, TestResponse.class );

		// Then
		String requestPath = mockWebServer.takeRequest().getPath();
		assertThat( requestPath ).contains( "existing=value" );
		assertThat( requestPath ).contains( "newKey=newValue" );
		assertThat( requestPath ).contains( "&" ); // & 로 연결되었는지
	}

	@Test
	@DisplayName("API 이름 - 로깅용: 올바른 이름 반환")
	void getApiName_ReturnsCorrectName() {
		// When
		String apiName = testApiClient.getApiName();

		// Then
		assertThat( apiName ).isEqualTo( "Test API" );
	}

	@Test
	@DisplayName("Exponential Backoff - 지수 백오프 확인")
	void exponentialBackoff_Verification() throws Exception {
		// Given
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse().setResponseCode( 500 ) );
		mockWebServer.enqueue( new MockResponse()
									   .setBody( "{\"message\":\"success\",\"code\":200}" )
									   .setResponseCode( 200 ) );

		// When
		long startTime = System.currentTimeMillis();
		testApiClient.get( "/test", TestResponse.class );
		long duration = System.currentTimeMillis() - startTime;

		// Then
		// 1차 실패 → 1초 대기 → 2차 실패 → 2초 대기 → 3차 성공
		// 총 대기 시간: 최소 3초 (1초 + 2초)
		assertThat( duration ).isGreaterThanOrEqualTo( 3000 );
		assertThat( duration ).isLessThan( 5000 );
	}
}
