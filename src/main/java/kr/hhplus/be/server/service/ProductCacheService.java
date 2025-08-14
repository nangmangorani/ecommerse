package kr.hhplus.be.server.service;

import kr.hhplus.be.server.dto.product.ResponseProduct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 캐시 키 상수들
    private static final String TOP5_CACHE_KEY = "products:top5";
    private static final String CATEGORY_PREFIX = "products:category:";
    private static final String PRODUCT_PREFIX = "product:";

    // TTL 설정
    private static final Duration TOP5_TTL = Duration.ofHours(1);
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(15);

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * TOP5 상품 캐시 조회
     */
    @SuppressWarnings("unchecked")
    public List<ResponseProduct> getTop5Products() {
        try {
            Object cached = redisTemplate.opsForValue().get(TOP5_CACHE_KEY);
            if (cached instanceof List<?> list) {
                log.debug("TOP5 상품 캐시 히트");
                return (List<ResponseProduct>) list;
            }
            log.debug("TOP5 상품 캐시 미스");
            return null;
        } catch (Exception e) {
            log.warn("TOP5 상품 캐시 조회 중 오류 발생", e);
            return null;
        }
    }

    /**
     * TOP5 상품 캐시 저장
     */
    public void setTop5Products(List<ResponseProduct> products) {
        if (products == null || products.isEmpty()) {
            log.warn("빈 상품 리스트는 캐시에 저장하지 않습니다");
            return;
        }

        try {
            redisTemplate.opsForValue().set(TOP5_CACHE_KEY, products, TOP5_TTL);
            log.info("TOP5 상품 캐시 저장 완료, 상품 수: {}, TTL: {}시간",
                    products.size(), TOP5_TTL.toHours());
        } catch (Exception e) {
            log.error("TOP5 상품 캐시 저장 중 오류 발생", e);
        }
    }

    /**
     * TOP5 캐시 무효화
     */
    public boolean invalidateTop5Cache() {
        try {
            Boolean deleted = redisTemplate.delete(TOP5_CACHE_KEY);
            boolean success = Boolean.TRUE.equals(deleted);
            log.info("TOP5 캐시 무효화 {}", success ? "성공" : "실패 (키가 존재하지 않음)");
            return success;
        } catch (Exception e) {
            log.error("TOP5 캐시 무효화 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 개별 상품 캐시 조회
     */
    public ResponseProduct getProduct(Long productId) {
        String key = PRODUCT_PREFIX + productId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ResponseProduct product) {
                log.debug("상품 {} 캐시 히트", productId);
                return product;
            }
            return null;
        } catch (Exception e) {
            log.warn("상품 {} 캐시 조회 중 오류 발생", productId, e);
            return null;
        }
    }

    /**
     * 개별 상품 캐시 저장
     */
    public void setProduct(ResponseProduct product) {
        if (product == null || product.productId() == null) {
            return;
        }

        String key = PRODUCT_PREFIX + product.productId();
        try {
            redisTemplate.opsForValue().set(key, product, PRODUCT_TTL);
            log.debug("상품 {} 캐시 저장 완료", product.productId());
        } catch (Exception e) {
            log.error("상품 {} 캐시 저장 중 오류 발생", product.productId(), e);
        }
    }

    /**
     * 개별 상품 캐시 무효화
     */
    public boolean invalidateProductCache(Long productId) {
        String key = PRODUCT_PREFIX + productId;
        try {
            Boolean deleted = redisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("상품 {} 캐시 무효화 중 오류 발생", productId, e);
            return false;
        }
    }

    /**
     * 인기상품 관련 캐시 모두 무효화 (TOP5)
     */
    public void invalidateAllPopularProductCaches() {
        try {
            Set<String> keys = Set.of(TOP5_CACHE_KEY);
            Long deletedCount = redisTemplate.delete(keys);
            log.info("인기상품 캐시 일괄 무효화 완료, 삭제된 키 수: {}", deletedCount);
        } catch (Exception e) {
            log.error("인기상품 캐시 일괄 무효화 중 오류 발생", e);
        }
    }

    /**
     * 특정 패턴의 캐시 모두 무효화
     */
    public void invalidateCachesByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.info("패턴 '{}' 캐시 무효화 완료, 삭제된 키 수: {}", pattern, deletedCount);
            } else {
                log.info("패턴 '{}'에 해당하는 캐시가 없습니다", pattern);
            }
        } catch (Exception e) {
            log.error("패턴 '{}' 캐시 무효화 중 오류 발생", pattern, e);
        }
    }

    /**
     * 모든 상품 관련 캐시 무효화 (위험한 메소드!)
     */
    public void invalidateAllProductCaches() {
        try {
            invalidateCachesByPattern("products:*");
            invalidateCachesByPattern("product:*");
            log.warn("모든 상품 캐시가 무효화되었습니다!");
        } catch (Exception e) {
            log.error("전체 상품 캐시 무효화 중 오류 발생", e);
        }
    }

    /**
     * 캐시 상태 정보 조회
     */
    public CacheStatus getCacheStatus() {
        return CacheStatus.builder()
                .top5Info(getCacheInfo(TOP5_CACHE_KEY))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 특정 키의 캐시 정보 조회
     */
    public CacheInfo getCacheInfo(String key) {
        try {
            boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(key));
            long ttlSeconds = exists ? redisTemplate.getExpire(key, TimeUnit.SECONDS) : -1;

            return CacheInfo.builder()
                    .key(key)
                    .exists(exists)
                    .ttlSeconds(ttlSeconds)
                    .expiryTime(ttlSeconds > 0 ?
                            LocalDateTime.now().plusSeconds(ttlSeconds) : null)
                    .build();
        } catch (Exception e) {
            log.warn("캐시 정보 조회 중 오류 발생, key: {}", key, e);
            return CacheInfo.builder()
                    .key(key)
                    .exists(false)
                    .ttlSeconds(-1)
                    .build();
        }
    }

    /**
     * 캐시 히트율 계산을 위한 메트릭 (간단한 예시)
     */
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);

    public void recordCacheHit() {
        cacheHitCount.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMissCount.incrementAndGet();
    }

    public CacheMetrics getCacheMetrics() {
        long hits = cacheHitCount.get();
        long misses = cacheMissCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0.0;

        return CacheMetrics.builder()
                .hitCount(hits)
                .missCount(misses)
                .totalCount(total)
                .hitRate(hitRate)
                .build();
    }

    /**
     * 캐시 메트릭 초기화
     */
    public void resetMetrics() {
        cacheHitCount.set(0);
        cacheMissCount.set(0);
        log.info("캐시 메트릭이 초기화되었습니다");
    }

    // ================== 내부 클래스들 ==================

    @Builder
    public record CacheInfo(
            String key,
            boolean exists,
            long ttlSeconds,
            LocalDateTime expiryTime
    ) {}

    @Builder
    public record CacheStatus(
            CacheInfo top5Info,
            LocalDateTime timestamp
    ) {}

    @Builder
    public record CacheMetrics(
            long hitCount,
            long missCount,
            long totalCount,
            double hitRate
    ) {}
}