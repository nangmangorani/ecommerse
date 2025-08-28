package kr.hhplus.be.server.eventHandler;

import kr.hhplus.be.server.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderCreatedEvent {
    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final Long originalPrice;
    private final Long requestPrice;
    private final LocalDateTime createdAt;
    private final String productName;
    private final String userName;

    public static OrderCreatedEvent of(Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getUser().getId(),
                order.getProduct().getId(),
                order.getRequestQuantity(),
                order.getOriginalPrice(),
                order.getDiscountedPrice(),
                order.getOrderDateTime(),
                order.getUser().getName(),
                order.getProduct().getName()

        );
    }
}
