package kr.hhplus.be.server.dto.order;

public record ResponseOrder(
        long userId,
        long productId,
        long couponId,
        String userName,
        String productName,
        String couponName,
        String couponYn,
        int originalPrice,
        int discountPrice
) {}
