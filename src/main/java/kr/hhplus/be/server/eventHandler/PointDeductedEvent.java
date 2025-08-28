package kr.hhplus.be.server.eventHandler;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PointDeductedEvent {
    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final Long originalPrice;
    private final Long requestPrice;
    private final LocalDateTime createdAt;
    private final String productName;
    private final String userName;


    public static PointDeductedEvent of(OrderCreatedEvent event) {
        return new PointDeductedEvent(
                event.getOrderId(),
                event.getUserId(),
                event.getProductId(),
                event.getRequestQuantity(),
                event.getOriginalPrice(),
                event.getRequestPrice(),
                event.getCreatedAt(),
                event.getUserName(),
                event.getProductName()
        );
    }
}
