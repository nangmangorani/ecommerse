package kr.hhplus.be.server.dto.coupon;

public record ResponseUserCoupon(
        long userId,
        long couponId,
        String userName,
        String couponName,
        double discountPercent
) {}

