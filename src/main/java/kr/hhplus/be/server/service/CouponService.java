package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.dto.coupon.ResponseUserCoupon;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String COUPON_COUNT_KEY = "coupon:count:";
    private static final String COUPON_BITMAP_KEY = "coupon:bitmap:";
    private static final String COUPON_TIMESTAMP_KEY = "coupon:timestamp:";


    @Transactional
    public ResponseUserCoupon getCoupon(RequestUserCoupon requestUserCoupon) {

        Long userId = requestUserCoupon.userId();
        Long couponId = requestUserCoupon.couponId();
        Long productId = requestUserCoupon.productId();

        if (hasUserIssuedCouponBitmap(userId, couponId)) {
            throw new CustomException("쿠폰을 이미 발급받았음");
        }

        String maxCountKey = "coupon:max:" + couponId;
        Integer maxCount = integerRedisTemplate.opsForValue().get(maxCountKey);

        if (maxCount == null) {
            Coupon coupon = searchCoupon(couponId);

            if (coupon.getRemainQuantity() <= 0) {
                throw new CustomException("쿠폰 소진");
            }

            maxCount = coupon.getMaxQuantity();
            integerRedisTemplate.opsForValue().set(maxCountKey, maxCount, Duration.ofDays(1));
        }

        String countKey = COUPON_COUNT_KEY + couponId;
        Long currentCount = longRedisTemplate.opsForValue().increment(countKey);

        if (currentCount == 1) {
            longRedisTemplate.expire(countKey, Duration.ofDays(7));
        }

        if (currentCount > maxCount) {
            longRedisTemplate.opsForValue().decrement(countKey);
            throw new CustomException("쿠폰이 모두 발급되었습니다.");
        }

        if (!setUserCouponIssuedBitmap(userId, couponId)) {
            longRedisTemplate.opsForValue().decrement(countKey);
            throw new CustomException("쿠폰을 이미 발급받았음");
        }

        saveIssuedTimestamp(userId, couponId);

        ResponseUserCoupon responseUserCoupon = new ResponseUserCoupon(userId, couponId, productId);

        return responseUserCoupon;
    }

    public Coupon searchCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException("쿠폰을 찾을 수 없음"));
    }

    public Coupon searchCouponByProductId(Long productId) {
        return couponRepository.findCouponByProductIdAndStatus(productId, CouponStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("상품에 부합한 쿠폰이 없음"));
    }

    public long calculateDiscountedPrice(Product product, Coupon coupon) {

        long expectedDiscountPrice;

        if (coupon != null) {
            expectedDiscountPrice = product.getPrice() - (product.getPrice() * coupon.getDiscountPercent() / 100);
        } else {
            expectedDiscountPrice = product.getPrice();
        }

        return expectedDiscountPrice;
    }



    /**
     * 비트맵용 추가 여기부터!!
     */

    /**
     * 비트맵으로 사용자 쿠폰 발급 여부 확인
     */
    private boolean hasUserIssuedCouponBitmap(Long userId, Long couponId) {
        String bitmapKey = COUPON_BITMAP_KEY + couponId;
        Boolean isIssued = stringRedisTemplate.opsForValue().getBit(bitmapKey, userId);
        return Boolean.TRUE.equals(isIssued);
    }

    /**
     * 비트맵에 사용자 쿠폰 발급 이력 저장
     */
    private boolean setUserCouponIssuedBitmap(Long userId, Long couponId) {
        String bitmapKey = COUPON_BITMAP_KEY + couponId;

        Boolean alreadyIssued = stringRedisTemplate.opsForValue().getBit(bitmapKey, userId);
        if (Boolean.TRUE.equals(alreadyIssued)) {
            return false;
        }

        stringRedisTemplate.opsForValue().setBit(bitmapKey, userId, true);

        stringRedisTemplate.expire(bitmapKey, Duration.ofDays(7));

        return true;
    }

    /**
     * 발급 시간 별도 저장 (배치 처리시 필요)
     */
    private void saveIssuedTimestamp(Long userId, Long couponId) {
        String timestampKey = COUPON_TIMESTAMP_KEY + couponId + ":" + userId;
        longRedisTemplate.opsForValue().set(
                timestampKey,
                System.currentTimeMillis(),
                Duration.ofDays(7)
        );
    }


}
