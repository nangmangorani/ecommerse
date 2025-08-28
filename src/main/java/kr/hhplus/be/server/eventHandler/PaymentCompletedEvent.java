package kr.hhplus.be.server.eventHandler;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentCompletedEvent {

    private final long paymentId;
    private final boolean paymentSuccess;
    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final Long originalPrice;
    private final Long requestPrice;
    private final LocalDateTime createdAt;
    private final String productName;
    private final String userName;

    public static PaymentCompletedEvent of(PointDeductedEvent event, long paymentId, boolean success) {
        return new PaymentCompletedEvent(
                paymentId,
                success,
                event.getOrderId(),
                event.getUserId(),
                event.getProductId(),
                event.getRequestQuantity(),
                event.getOriginalPrice(),
                event.getRequestPrice(),
                event.getCreatedAt(),
                event.getProductName(),
                event.getUserName()
        );
    }

    public static PaymentCompletedEvent success(PointDeductedEvent event, long paymentId) {
        return of(event, paymentId, true);
    }

    public static PaymentCompletedEvent failure(PointDeductedEvent event, long paymentId) {
        return of(event, paymentId, false);
    }

}

