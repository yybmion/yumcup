package mioneF.yumCup.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Kakao Local API 클라이언트 구현체
 */
@Component
public class KakaoLocalApiClient extends AbstractWebClientApiClient {

	private static final String CATEGORY_GROUP_CODE = "FD6";
	private static final int PAGE_SIZE = 15;

	public KakaoLocalApiClient(
			@Qualifier("kakaoWebClient") WebClient webClient,
			ObjectMapper objectMapper) {
		super( webClient, objectMapper );
	}

	/**
	 * 카테고리 기반 장소 검색
	 *
	 * <p>주어진 위치 주변의 음식점을 검색합니다.</p>
	 *
	 * @param latitude 중심 위도
	 * @param longitude 중심 경도
	 * @param radius 반경 (미터)
	 * @param page 페이지 번호 (1부터 시작)
	 * @param responseType 응답 타입 클래스
	 * @param <T> 응답 타입
	 *
	 * @return 파싱된 응답 객체
	 */
	public <T> T searchByCategory(
			double latitude,
			double longitude,
			int radius,
			int page,
			Class<T> responseType) {

		Map<String, String> params = buildCategorySearchParams( latitude, longitude, radius, page );
		return get( "/v2/local/search/category.json", params, responseType );
	}

	/**
	 * 카테고리 검색 쿼리 파라미터 생성
	 *
	 * @param latitude 위도
	 * @param longitude 경도
	 * @param radius 반경 (미터)
	 * @param page 페이지 번호
	 *
	 * @return 쿼리 파라미터 Map
	 */
	private Map<String, String> buildCategorySearchParams(
			double latitude,
			double longitude,
			int radius,
			int page) {

		Map<String, String> params = new HashMap<>();
		params.put( "category_group_code", CATEGORY_GROUP_CODE );
		params.put( "x", String.valueOf( longitude ) );
		params.put( "y", String.valueOf( latitude ) );
		params.put( "radius", String.valueOf( radius ) );
		params.put( "size", String.valueOf( PAGE_SIZE ) );
		params.put( "page", String.valueOf( page ) );

		return params;
	}

	@Override
	protected String getApiName() {
		return "Kakao Local API";
	}
}
