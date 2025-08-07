package kr.hhplus.be.server.dto.order;

public record RequestOrder(
        Long userId,
        Long productId,
        Long couponId,
        int requestQuantity,
        long originalPrice,
        long requestPrice,
        boolean couponYn
) {}
