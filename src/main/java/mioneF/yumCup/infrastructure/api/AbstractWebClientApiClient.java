package mioneF.yumCup.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mioneF.yumCup.exception.ExternalApiException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * WebClient 기반 API 클라이언트 추상 클래스
 */
@Slf4j
public abstract class AbstractWebClientApiClient implements ExternalApiClient {

	protected final WebClient webClient;
	protected final ObjectMapper objectMapper;

	private static final int TIMEOUT_SECONDS = 10;
	private static final int MAX_RETRY = 3;
	private static final long BASE_RETRY_DELAY_MS = 1000L;

	protected AbstractWebClientApiClient(WebClient webClient, ObjectMapper objectMapper) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * GET 요청 수행 (URL만)
	 */
	@Override
	public <T> T get(String url, Class<T> responseType) {
		return executeWithRetry( () -> performGet( url, responseType ) );
	}

	/**
	 * GET 요청 수행 (URL + 쿼리 파라미터)
	 */
	@Override
	public <T> T get(String baseUrl, Map<String, String> queryParams, Class<T> responseType) {
		String url = buildUrlWithParams( baseUrl, queryParams );
		return get( url, responseType );
	}

	/**
	 * POST 요청 수행
	 */
	@Override
	public <T, R> R post(String url, T requestBody, Class<R> responseType) {
		return executeWithRetry( () -> performPost( url, requestBody, responseType ) );
	}

	/**
	 * 실제 GET 요청 수행
	 */
	protected <T> T performGet(String url, Class<T> responseType) {
		try {
			log.debug( "[{}] API GET Request: {}", getApiName(), url );

			String response = webClient.get()
					.uri( url )
					.retrieve()
					.bodyToMono( String.class )
					.timeout( Duration.ofSeconds( TIMEOUT_SECONDS ) )
					.block();

			log.debug(
					"[{}] API Response received: {} bytes",
					getApiName(),
					response != null ? response.length() : 0
			);

			return objectMapper.readValue( response, responseType );

		}
		catch (WebClientResponseException e) {
			log.error(
					"[{}] API call failed: {} - {}",
					getApiName(),
					e.getStatusCode(),
					e.getResponseBodyAsString()
			);
			throw new ExternalApiException(
					String.format( "%s API call failed: %s", getApiName(), e.getMessage() ),
					e
			);
		}
		catch (Exception e) {
			log.error( "[{}] Unexpected error during API call: {}", getApiName(), e.getMessage() );
			throw new ExternalApiException(
					String.format( "%s unexpected error: %s", getApiName(), e.getMessage() ),
					e
			);
		}
	}

	/**
	 * 실제 POST 요청 수행
	 */
	protected <T, R> R performPost(String url, T requestBody, Class<R> responseType) {
		try {
			log.debug( "[{}] API POST Request: {}", getApiName(), url );

			String response = webClient.post()
					.uri( url )
					.bodyValue( requestBody )
					.retrieve()
					.bodyToMono( String.class )
					.timeout( Duration.ofSeconds( TIMEOUT_SECONDS ) )
					.block();

			return objectMapper.readValue( response, responseType );

		}
		catch (WebClientResponseException e) {
			log.error(
					"[{}] API call failed: {} - {}",
					getApiName(),
					e.getStatusCode(),
					e.getResponseBodyAsString()
			);
			throw new ExternalApiException(
					String.format( "%s API call failed: %s", getApiName(), e.getMessage() ),
					e
			);
		}
		catch (Exception e) {
			log.error( "[{}] Unexpected error: {}", getApiName(), e.getMessage() );
			throw new ExternalApiException(
					String.format( "%s unexpected error: %s", getApiName(), e.getMessage() ),
					e
			);
		}
	}

	/**
	 * 재시도 로직 (Exponential Backoff)
	 */
	private <T> T executeWithRetry(Supplier<T> apiCall) {
		Exception lastException = null;

		for ( int attempt = 1; attempt <= MAX_RETRY; attempt++ ) {
			try {
				return apiCall.get();
			}
			catch (Exception e) {
				lastException = e;
				log.warn(
						"[{}] API call attempt {}/{} failed: {}",
						getApiName(),
						attempt,
						MAX_RETRY,
						e.getMessage()
				);

				if ( attempt < MAX_RETRY ) {
					long delayMs = BASE_RETRY_DELAY_MS * attempt;
					try {
						log.debug( "[{}] Waiting {}ms before retry...", getApiName(), delayMs );
						Thread.sleep( delayMs );
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new ExternalApiException( "Retry interrupted", ie );
					}
				}
			}
		}

		log.error( "[{}] API call failed after {} attempts", getApiName(), MAX_RETRY );
		throw new ExternalApiException(
				String.format( "%s API call failed after %d attempts", getApiName(), MAX_RETRY ),
				lastException
		);
	}

	/**
	 * URL에 쿼리 파라미터 추가
	 */
	private String buildUrlWithParams(String baseUrl, Map<String, String> params) {
		if ( params == null || params.isEmpty() ) {
			return baseUrl;
		}

		StringBuilder urlBuilder = new StringBuilder( baseUrl );
		urlBuilder.append( baseUrl.contains( "?" ) ? "&" : "?" );

		params.forEach( (key, value) ->
								urlBuilder.append( key ).append( "=" ).append( value ).append( "&" )
		);

		// 마지막 "&" 제거
		return urlBuilder.substring( 0, urlBuilder.length() - 1 );
	}

	/**
	 * API 이름 반환 (로깅용)
	 */
	protected abstract String getApiName();
}
