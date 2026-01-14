package mioneF.yumCup.infrastructure.cache;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

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

	/**
	 * 패턴 기반 캐시 삭제 (SCAN 사용으로 메모리 최적화)
	 * KEYS 명령어 대신 SCAN을 사용하여 대규모 Redis에서도 안전하게 동작
	 */
	@Override
	public void evictByPattern(String pattern) {
		try {
			ScanOptions options = ScanOptions.scanOptions()
					.match( pattern )
					.count( 100 )
					.build();

			long deletedCount = 0;
			List<String> keysToDelete = new ArrayList<>();

			try (Cursor<String> cursor = redisTemplate.scan( options )) {
				while ( cursor.hasNext() ) {
					keysToDelete.add( cursor.next() );

					// 배치 삭제: 100개씩 모아서 삭제하여 메모리 사용량 제한
					if ( keysToDelete.size() >= 100 ) {
						Long deleted = redisTemplate.delete( keysToDelete );
						deletedCount += deleted != null ? deleted : 0;
						keysToDelete.clear();
					}
				}
			}

			// 남은 키 삭제
			if ( !keysToDelete.isEmpty() ) {
				Long deleted = redisTemplate.delete( keysToDelete );
				deletedCount += deleted != null ? deleted : 0;
			}

			log.debug( "Cache evicted by pattern: {} (deleted: {})", pattern, deletedCount );
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
