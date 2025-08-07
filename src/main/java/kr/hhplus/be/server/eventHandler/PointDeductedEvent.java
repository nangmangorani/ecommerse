package kr.hhplus.be.server.eventHandler;

import lombok.Getter;

@Getter
public class PointDeductedEvent {
    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final long requestPrice;

    private PointDeductedEvent(Long orderId, Long userId, Long productId,
                               int requestQuantity, long requestPrice) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.requestQuantity = requestQuantity;
        this.requestPrice = requestPrice;
    }

    public static PointDeductedEvent of(OrderCreatedEvent event) {
        return new PointDeductedEvent(
                event.getOrderId(),
                event.getUserId(),
                event.getProductId(),
                event.getRequestQuantity(),
                event.getRequestPrice()
        );
    }
}
