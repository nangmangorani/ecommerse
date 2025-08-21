package kr.hhplus.be.server.integrationTest;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.repository.CouponHistRepository;
import kr.hhplus.be.server.repository.CouponRepository;
import kr.hhplus.be.server.service.CouponHistService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@Slf4j
public class CouponHistBatchTest {

    @Autowired
    private CouponHistService couponHistService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponHistRepository couponHistRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String COUPON_BITMAP_KEY = "coupon:bitmap:";
    private static final String COUPON_TIMESTAMP_KEY = "coupon:timestamp:";
    private static final String COUPON_COUNT_KEY = "coupon:count:";

    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        couponRepository.deleteAll();
        couponHistRepository.deleteAll();

        testCoupon = new Coupon(
                "배치테스트쿠폰",
                CouponStatus.ACTIVE,
                20,
                100,
                100,
                1L
        );
        testCoupon = couponRepository.save(testCoupon);
    }

    @Test
    @DisplayName("Redis 비트맵에서 DB로 쿠폰 이력 저장 테스트")
    void saveCouponHistoriesFromBitmapOptimizedTest() {
        Long couponId = testCoupon.getId();
        Long[] userIds = {1001L, 1002L, 1003L, 1004L, 1005L};

        String bitmapKey = COUPON_BITMAP_KEY + couponId;
        String countKey = COUPON_COUNT_KEY + couponId;

        for (Long userId : userIds) {
            redisTemplate.opsForValue().setBit(bitmapKey, userId, true);

            String timestampKey = COUPON_TIMESTAMP_KEY + couponId + ":" + userId;
            redisTemplate.opsForValue().set(timestampKey, System.currentTimeMillis(), Duration.ofDays(7));
        }

        redisTemplate.opsForValue().set(countKey, userIds.length, Duration.ofDays(7));

        List<CouponHist> beforeBatch = couponHistRepository.findAll();
        assertThat(beforeBatch).isEmpty();

        long startTime = System.currentTimeMillis();

        Set<String> timestampKeys = redisTemplate.keys(COUPON_TIMESTAMP_KEY + couponId + ":*");
        couponHistService.saveCouponHistoriesFromTimestamps(timestampKeys);

        long processingTime = System.currentTimeMillis() - startTime;

        List<CouponHist> afterBatch = couponHistRepository.findAll();
        assertThat(afterBatch).hasSize(userIds.length);

        log.info("=== 최적화된 배치 처리 결과 ===");
        log.info("처리 데이터: {}명", userIds.length);
        log.info("처리 시간: {}ms", processingTime);
        log.info("DB 저장 완료: {}건", afterBatch.size());
    }

    @Test
    @DisplayName("타임스탬프 키 기반 배치 처리 테스트 (빠른 방식)")
    void saveCouponHistoriesFromTimestampsOptimizedTest() {
        Long couponId = testCoupon.getId();
        Long[] userIds = {2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L, 2008L, 2009L, 2010L};

        long todayTimestamp = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        long setupStartTime = System.currentTimeMillis();

        for (Long userId : userIds) {
            String timestampKey = COUPON_TIMESTAMP_KEY + couponId + ":" + userId;
            redisTemplate.opsForValue().set(timestampKey, todayTimestamp, Duration.ofDays(7));

            String bitmapKey = COUPON_BITMAP_KEY + couponId;
            redisTemplate.opsForValue().setBit(bitmapKey, userId, true);
        }

        long setupTime = System.currentTimeMillis() - setupStartTime;

        long batchStartTime = System.currentTimeMillis();

        Set<String> allTimestampKeys = redisTemplate.keys(COUPON_TIMESTAMP_KEY + couponId + ":*");
        couponHistService.saveCouponHistoriesFromTimestamps(allTimestampKeys);

        long batchTime = System.currentTimeMillis() - batchStartTime;

        List<CouponHist> savedHist = couponHistRepository.findAll();
        assertThat(savedHist).hasSize(userIds.length);

        log.info("=== 타임스탬프 기반 배치 결과 ===");
        log.info("데이터 설정 시간: {}ms", setupTime);
        log.info("배치 처리 시간: {}ms", batchTime);
        log.info("처리된 키: {}개", allTimestampKeys.size());
        log.info("DB 저장 완료: {}건", savedHist.size());
        log.info("평균 처리 속도: {:.2f} 건/초", (savedHist.size() * 1000.0 / batchTime));
    }

}