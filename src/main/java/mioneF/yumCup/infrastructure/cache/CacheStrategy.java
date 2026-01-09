package mioneF.yumCup.infrastructure.cache;

import java.util.Optional;

/**
 * 캐싱 전략 인터페이스
 */
public interface CacheStrategy {

	/**
	 * 캐시에서 데이터 조회
	 */
	<T> Optional<T> get(String key, Class<T> type);

	/**
	 * 캐시에 데이터 저장 (기본 TTL 사용)
	 */
	void put(String key, Object value);

	/**
	 * 캐시에 데이터 저장 (TTL 지정)
	 */
	void put(String key, Object value, long ttlSeconds);

	/**
	 * 캐시에서 데이터 삭제
	 */
	void evict(String key);

	/**
	 * 패턴에 맞는 모든 키 삭제
	 */
	void evictByPattern(String pattern);

	/**
	 * 캐시 전체 삭제
	 */
	void clear();

	/**
	 * 캐시 키 존재 여부 확인
	 */
	boolean exists(String key);
}
