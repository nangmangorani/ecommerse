package kr.hhplus.be.dto.order;

public record RequestOrder(
        long userId,
        long productId,
        long couponId,
        int requestQuantity,
        long originalPrice,
        long requestPrice,
        String couponYn
) {}
