package mioneF.yumCup.infrastructure.api;

import java.util.Map;

public interface ExternalApiClient {

	/**
	 * GET 요청 수행
	 */
	<T> T get(String url, Class<T> responseType);

	/**
	 * GET 요청 수행 (쿼리 파라미터 포함)
	 */
	<T> T get(String baseUrl, Map<String, String> queryParams, Class<T> responseType);

	/**
	 * POST 요청 수행
	 */
	<T, R> R post(String url, T requestBody, Class<R> responseType);
}
