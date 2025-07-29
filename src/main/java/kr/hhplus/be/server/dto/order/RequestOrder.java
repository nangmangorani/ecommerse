package kr.hhplus.be.server.dto.order;

public record RequestOrder(
        long userId,
        long productId,
        long couponId,
        int requestQuantity,
        long originalPrice,
        long requestPrice,
        boolean couponYn
) {}
