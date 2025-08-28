package kr.hhplus.be.server.batch;

import kr.hhplus.be.server.service.CouponHistService;
import kr.hhplus.be.server.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponHistBatch {

    private final CouponHistService couponHistService;
    private final CouponService couponService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;

    private static final String COUPON_BITMAP_KEY = "coupon:bitmap:";
    private static final String COUPON_TIMESTAMP_KEY = "coupon:timestamp:";

    @Scheduled(cron = "0 0 3 * * *")
    public void saveDailyIssuedCoupons() {
        log.info("일일 쿠폰 발급 내역 DB 저장 시작");

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            processCouponsByDate(yesterday);
        } catch (Exception e) {
            log.error("일일 쿠폰 DB 저장 실패", e);
        }
    }

    private void processCouponsByDate(LocalDate targetDate) {
        String datePattern = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Set<String> timestampKeys = longRedisTemplate.keys(COUPON_TIMESTAMP_KEY + "*");
        Set<String> targetDateKeys = new HashSet<>();

        if (!timestampKeys.isEmpty()) {
            for (String key : timestampKeys) {
                Long timestamp = longRedisTemplate.opsForValue().get(key);
                if (timestamp != null) {
                    LocalDate issueDate = LocalDate.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                    );

                    if (issueDate.equals(targetDate)) {
                        targetDateKeys.add(key);
                    }
                }
            }
        }

        if (!targetDateKeys.isEmpty()) {
            couponHistService.saveCouponHistoriesFromTimestamps(targetDateKeys);

            longRedisTemplate.delete(targetDateKeys);

            log.info("날짜별 쿠폰 DB 저장 완료: {} ({} 건)", targetDate, targetDateKeys.size());
        } else {
            log.info("저장할 쿠폰 데이터 없음: {}", targetDate);
        }
    }

    public void saveAllRemainingCoupons(Long couponId) {
        log.info("쿠폰 이벤트 종료 - 잔여 발급분 DB 저장: {}", couponId);

        try {
            couponHistService.saveCouponHistoriesFromBitmap(couponId);

            cleanupRedisData(couponId);

            log.info("잔여 쿠폰 DB 저장 완료: {}", couponId);
        } catch (Exception e) {
            log.error("잔여 쿠폰 DB 저장 실패: {}", couponId, e);
        }
    }

    private void cleanupRedisData(Long couponId) {
        try {
            String bitmapKey = COUPON_BITMAP_KEY + couponId;
            stringRedisTemplate.delete(bitmapKey);

            String timestampPattern = COUPON_TIMESTAMP_KEY + couponId + ":*";
            Set<String> timestampKeys = longRedisTemplate.keys(timestampPattern);
            if (!timestampKeys.isEmpty()) {
                longRedisTemplate.delete(timestampKeys);
            }

            String countKey = "coupon:count:" + couponId;
            longRedisTemplate.delete(countKey);

            log.info("Redis 데이터 정리 완료: {}", couponId);
        } catch (Exception e) {
            log.error("Redis 데이터 정리 실패: {}", couponId, e);
        }
    }
}