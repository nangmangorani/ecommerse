package kr.hhplus.be.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 키 상수들
    private static final String TOP5_CACHE_KEY = "products:top5";
    private static final String CATEGORY_PREFIX = "products:category:";
    private static final String PRODUCT_PREFIX = "product:";

    // TTL 설정
    private static final Duration TOP5_TTL = Duration.ofHours(1);
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(15);

    public ProductCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * TOP5 상품 캐시 조회
     */
    @SuppressWarnings("unchecked")
    public List<ResponseProduct> getTop5Products() {
        try {
            Object cached = redisTemplate.opsForValue().get(TOP5_CACHE_KEY);
            if (cached == null) {
                log.debug("TOP5 상품 캐시 미스");
                return null;
            }

            if (cached instanceof String jsonString) {
                // JSON 문자열로 저장된 경우
                TypeReference<List<ResponseProduct>> typeRef = new TypeReference<List<ResponseProduct>>() {};
                List<ResponseProduct> products = objectMapper.readValue(jsonString, typeRef);
                log.debug("TOP5 상품 캐시 히트 (JSON 파싱)");
                return products;
            } else if (cached instanceof List<?> list) {
                List<ResponseProduct> products = list.stream()
                        .map(this::convertToResponseProduct)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                if (!products.isEmpty()) {
                    log.debug("TOP5 상품 캐시 히트 (객체 변환)");
                    return products;
                }
            }

            log.warn("캐시에서 올바르지 않은 타입 반환: {}", cached.getClass().getName());
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

    // 캐시 객체 -> DTO 변환
    private ResponseProduct convertToResponseProduct(Object obj) {
        try {
            if (obj instanceof ResponseProduct) {
                return (ResponseProduct) obj;  // 이미 올바른 타입
            } else if (obj instanceof Map<?, ?> map) {
                // LinkedHashMap -> ResponseProduct 변환
                String json = objectMapper.writeValueAsString(map);
                return objectMapper.readValue(json, ResponseProduct.class);
            }
            return null;
        } catch (Exception e) {
            log.warn("객체 변환 실패: {}", obj.getClass().getName(), e);
            return null;
        }
    }
}