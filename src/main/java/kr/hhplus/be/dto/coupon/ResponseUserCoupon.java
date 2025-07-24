package kr.hhplus.be.dto.coupon;

public record ResponseUserCoupon(
        long userId,
        long couponId,
        String userName,
        String couponName,
        double discountPercent
) {}

