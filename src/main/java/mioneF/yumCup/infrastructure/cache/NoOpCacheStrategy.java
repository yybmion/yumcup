package mioneF.yumCup.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * No-Operation 캐시 전략 (테스트용)
 */
@Slf4j
@Component
@Profile("test")
public class NoOpCacheStrategy implements CacheStrategy {

	public NoOpCacheStrategy() {
		log.info( "NoOpCacheStrategy activated - Cache disabled for testing" );
	}

	@Override
	public <T> Optional<T> get(String key, Class<T> type) {
		log.trace( "NoOp cache get: {}", key );
		return Optional.empty();
	}

	@Override
	public void put(String key, Object value) {
		log.trace( "NoOp cache put: {}", key );
	}

	@Override
	public void put(String key, Object value, long ttlSeconds) {
		log.trace( "NoOp cache put with TTL: {} ({}s)", key, ttlSeconds );
	}

	@Override
	public void evict(String key) {
		log.trace( "NoOp cache evict: {}", key );
	}

	@Override
	public void evictByPattern(String pattern) {
		log.trace( "NoOp cache evict by pattern: {}", pattern );
	}

	@Override
	public void clear() {
		log.trace( "NoOp cache clear" );
	}

	@Override
	public boolean exists(String key) {
		log.trace( "NoOp cache exists: {}", key );
		return false;
	}
}
