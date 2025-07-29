package kr.hhplus.be.server.dto.order;

public record ResponseOrder(
        String userName,
        String productName,
        String couponName,
        boolean couponYn,
        long originalPrice,
        long discountPrice
) {}
