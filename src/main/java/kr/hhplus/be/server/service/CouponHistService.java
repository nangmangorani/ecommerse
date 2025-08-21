package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.enums.CouponHistStatus;
import kr.hhplus.be.server.repository.CouponHistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponHistService {

    private final CouponHistRepository couponHistRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUPON_TIMESTAMP_KEY = "coupon:timestamp:";

    /**
     * 특정 쿠폰의 비트맵에서 발급받은 사용자들을 DB에 저장
     */
    @Transactional
    public void saveCouponHistoriesFromBitmap(Long couponId) {
        log.info("쿠폰 {} 비트맵에서 DB로 이력 저장 시작", couponId);

        Set<Long> issuedUserIds = getIssuedUserIdsFromBitmap(couponId);

        if (issuedUserIds.isEmpty()) {
            log.info("쿠폰 {} - 발급받은 사용자 없음", couponId);
            return;
        }

        List<CouponHist> batchData = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long userId : issuedUserIds) {
            try {
                if (couponHistRepository.findByCouponIdAndUserId(couponId, userId).isPresent()) {
                    log.debug("이미 DB에 존재하는 쿠폰 이력: userId={}, couponId={}", userId, couponId);
                    continue;
                }

                String timestampKey = COUPON_TIMESTAMP_KEY + couponId + ":" + userId;
                Long issuedTimestamp = (Long) redisTemplate.opsForValue().get(timestampKey);

                if (issuedTimestamp == null) {
                    log.warn("발급 시간을 찾을 수 없습니다: userId={}, couponId={}", userId, couponId);
                    issuedTimestamp = System.currentTimeMillis();
                }

                CouponHist couponHist = CouponHist.builder()
                        .userId(userId)
                        .couponId(couponId)
                        .issuedDateTime(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(issuedTimestamp),
                                ZoneId.systemDefault()
                        ))
                        .couponStatus(CouponHistStatus.ISSUED)
                        .build();

                batchData.add(couponHist);
                successCount++;

            } catch (Exception e) {
                log.error("개별 쿠폰 저장 실패: userId={}, couponId={}", userId, couponId, e);
                failCount++;
            }
        }

        if (!batchData.isEmpty()) {
            couponHistRepository.saveAll(batchData);
            log.debug("배치 저장 완료: {}건", batchData.size());
        }

        log.info("쿠폰 {} 이력 저장 완료 - 성공: {}, 실패: {}", couponId, successCount, failCount);
    }

    /**
     * 날짜별 타임스탬프 키들을 기반으로 DB 저장 - 배치 처리 최적화
     */
    @Transactional
    public void saveCouponHistoriesFromTimestamps(Set<String> timestampKeys) {
        if (timestampKeys == null || timestampKeys.isEmpty()) {
            log.info("저장할 타임스탬프 키가 없습니다.");
            return;
        }

        log.info("타임스탬프 키에서 DB로 쿠폰 이력 저장 시작: {} 건", timestampKeys.size());

        List<CouponHist> batchData = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (String timestampKey : timestampKeys) {
            try {
                CouponTimestampKeyInfo keyInfo = parseTimestampKey(timestampKey);

                if (couponHistRepository.findByCouponIdAndUserId(keyInfo.couponId(), keyInfo.userId()).isPresent()) {
                    log.debug("이미 DB에 존재하는 쿠폰 이력: userId={}, couponId={}", keyInfo.userId(), keyInfo.couponId());
                    continue;
                }

                Long issuedTimestamp = (Long) redisTemplate.opsForValue().get(timestampKey);
                if (issuedTimestamp == null) {
                    log.warn("발급 시간을 찾을 수 없습니다: userId={}, couponId={}", keyInfo.userId(), keyInfo.couponId());
                    issuedTimestamp = System.currentTimeMillis();
                }

                CouponHist couponHist = CouponHist.builder()
                        .userId(keyInfo.userId())
                        .couponId(keyInfo.couponId())
                        .issuedDateTime(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(issuedTimestamp),
                                ZoneId.systemDefault()
                        ))
                        .couponStatus(CouponHistStatus.ISSUED)
                        .build();

                batchData.add(couponHist);
                successCount++;

            } catch (Exception e) {
                log.error("개별 쿠폰 처리 실패: {}", timestampKey, e);
                failCount++;
            }
        }

        if (!batchData.isEmpty()) {
            couponHistRepository.saveAll(batchData);
            log.debug("배치 저장 완료: {}건", batchData.size());
        }

        log.info("쿠폰 이력 저장 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 비트맵에서 발급받은 사용자 ID 목록 추출 - 타임스탬프 키 기반으로 최적화
     */
    private Set<Long> getIssuedUserIdsFromBitmap(Long couponId) {
        Set<String> timestampKeys = redisTemplate.keys(COUPON_TIMESTAMP_KEY + couponId + ":*");

        Set<Long> issuedUserIds = new HashSet<>();
        for (String timestampKey : timestampKeys) {
            try {
                String[] parts = timestampKey.split(":");
                if (parts.length >= 4) {
                    Long userId = Long.parseLong(parts[3]);
                    issuedUserIds.add(userId);
                }
            } catch (NumberFormatException e) {
                log.warn("타임스탬프 키에서 userId 파싱 실패: {}", timestampKey);
            }
        }

        log.debug("쿠폰 {} - 발급받은 사용자 {}명 추출 완료", couponId, issuedUserIds.size());
        return issuedUserIds;
    }

    /**
     * 타임스탬프 키 파싱: "coupon:timestamp:couponId:userId"
     */
    private CouponTimestampKeyInfo parseTimestampKey(String timestampKey) {
        String[] parts = timestampKey.split(":");

        if (parts.length < 4) {
            throw new IllegalArgumentException("잘못된 타임스탬프 키 형식: " + timestampKey);
        }

        try {
            Long couponId = Long.parseLong(parts[2]);
            Long userId = Long.parseLong(parts[3]);
            return new CouponTimestampKeyInfo(userId, couponId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("타임스탬프 키 파싱 실패: " + timestampKey, e);
        }
    }
    private record CouponTimestampKeyInfo(Long userId, Long couponId) {}

}
