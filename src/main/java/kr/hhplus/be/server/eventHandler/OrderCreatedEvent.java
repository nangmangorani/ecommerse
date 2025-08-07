package kr.hhplus.be.server.eventHandler;

import kr.hhplus.be.server.domain.Order;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OrderCreatedEvent {
    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final long requestPrice;
    private final LocalDateTime createdAt;

    private OrderCreatedEvent(Long orderId, Long userId, Long productId,
                              int requestQuantity, long requestPrice) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.requestQuantity = requestQuantity;
        this.requestPrice = requestPrice;
        this.createdAt = LocalDateTime.now();
    }

    public static OrderCreatedEvent of(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getRequestQuantity(),
                order.getDiscountedPrice()
        );
    }
}
