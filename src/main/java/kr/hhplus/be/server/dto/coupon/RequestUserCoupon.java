package kr.hhplus.be.server.dto.coupon;

public record RequestUserCoupon(
        long userId,
        long couponId,
        long productId
) {}
