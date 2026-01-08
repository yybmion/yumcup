package mioneF.yumCup.infrastructure.api;

import java.util.Map;

public interface ExternalApiClient {

	/**
	 * GET 요청 수행
	 *
	 * @param url 요청 URL
	 * @param responseType 응답 타입
	 *
	 * @return 파싱된 응답 객체
	 *
	 * @throws ExternalApiException API 호출 실패 시
	 */
	<T> T get(String url, Class<T> responseType);

	/**
	 * GET 요청 수행 (쿼리 파라미터 포함)
	 *
	 * @param baseUrl 기본 URL
	 * @param queryParams 쿼리 파라미터
	 * @param responseType 응답 타입
	 *
	 * @return 파싱된 응답 객체
	 */
	<T> T get(String baseUrl, Map<String, String> queryParams, Class<T> responseType);

	/**
	 * POST 요청 수행
	 */
	<T, R> R post(String url, T requestBody, Class<R> responseType);
}
