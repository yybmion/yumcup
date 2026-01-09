package mioneF.yumCup.infrastructure.cache;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Geohash 기반 Redis 캐싱 전략
 */
@Slf4j
@Component
public class GeohashCacheStrategy implements CacheStrategy {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final long DEFAULT_TTL_SECONDS = 3600;
	private static final int DEFAULT_GEOHASH_PRECISION = 6;

	public GeohashCacheStrategy(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> Optional<T> get(String key, Class<T> type) {
		try {
			String cached = redisTemplate.opsForValue().get( key );

			if ( cached == null ) {
				log.debug( "Cache miss: {}", key );
				return Optional.empty();
			}

			log.debug( "Cache hit: {}", key );
			T value = objectMapper.readValue( cached, type );
			return Optional.of( value );

		}
		catch (JsonProcessingException e) {
			log.error( "Failed to deserialize cached value for key: {}", key, e );
			evict( key );
			return Optional.empty();
		}
		catch (Exception e) {
			log.error( "Failed to get cached value for key: {}", key, e );
			return Optional.empty();
		}
	}

	@Override
	public void put(String key, Object value) {
		put( key, value, DEFAULT_TTL_SECONDS );
	}

	@Override
	public void put(String key, Object value, long ttlSeconds) {
		try {
			String json = objectMapper.writeValueAsString( value );
			redisTemplate.opsForValue().set( key, json, ttlSeconds, TimeUnit.SECONDS );

			log.debug( "Cache stored: {} (TTL: {}s)", key, ttlSeconds );

		}
		catch (JsonProcessingException e) {
			log.error( "Failed to serialize value for key: {}", key, e );
		}
		catch (Exception e) {
			log.error( "Failed to store cache for key: {}", key, e );
		}
	}

	@Override
	public void evict(String key) {
		try {
			Boolean deleted = redisTemplate.delete( key );
			if ( Boolean.TRUE.equals( deleted ) ) {
				log.debug( "Cache evicted: {}", key );
			}
		}
		catch (Exception e) {
			log.error( "Failed to evict cache for key: {}", key, e );
		}
	}

	@Override
	public void evictByPattern(String pattern) {
		try {
			Set<String> keys = redisTemplate.keys( pattern );
			if ( keys != null && !keys.isEmpty() ) {
				Long deleted = redisTemplate.delete( keys );
				log.debug( "Cache evicted by pattern: {} (deleted: {})", pattern, deleted );
			}
		}
		catch (Exception e) {
			log.error( "Failed to evict cache by pattern: {}", pattern, e );
		}
	}

	@Override
	public void clear() {
		try {
			redisTemplate.getConnectionFactory()
					.getConnection()
					.flushDb();
			log.warn( "All cache cleared!" );
		}
		catch (Exception e) {
			log.error( "Failed to clear all cache", e );
		}
	}

	@Override
	public boolean exists(String key) {
		try {
			return Boolean.TRUE.equals( redisTemplate.hasKey( key ) );
		}
		catch (Exception e) {
			log.error( "Failed to check cache existence for key: {}", key, e );
			return false;
		}
	}

	/**
	 * Geohash 기반 캐시 키 생성
	 */
	public String generateGeohashKey(
			String prefix,
			double latitude,
			double longitude,
			String additional) {

		String geohash = GeoHash.withCharacterPrecision(
				latitude,
				longitude,
				DEFAULT_GEOHASH_PRECISION
		).toBase32();

		return String.format( "%s:geohash:%s:%s", prefix, geohash, additional );
	}

	/**
	 * Geohash 기반 캐시 키 생성 (추가 정보 없음)
	 */
	public String generateGeohashKey(String prefix, double latitude, double longitude) {
		String geohash = GeoHash.withCharacterPrecision(
				latitude,
				longitude,
				DEFAULT_GEOHASH_PRECISION
		).toBase32();

		return String.format( "%s:geohash:%s", prefix, geohash );
	}
}
