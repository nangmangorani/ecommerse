package kr.hhplus.be.server.eventHandler;

import lombok.Getter;

@Getter
public class PaymentCompletedEvent {

    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int requestQuantity;
    private final long requestPrice;
    private final long paymentId;
    private final boolean paymentSuccess;

    private PaymentCompletedEvent(Long orderId, Long userId, Long productId,
                                  int requestQuantity, long requestPrice,
                                  long paymentId, boolean paymentSuccess) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.requestQuantity = requestQuantity;
        this.requestPrice = requestPrice;
        this.paymentId = paymentId;
        this.paymentSuccess = paymentSuccess;
    }

    public static PaymentCompletedEvent of(PointDeductedEvent event, long paymentId, boolean success) {
        return new PaymentCompletedEvent(
                event.getOrderId(),
                event.getUserId(),
                event.getProductId(),
                event.getRequestQuantity(),
                event.getRequestPrice(),
                paymentId,
                success
        );
    }

    // 성공한 결제를 위한 편의 메소드
    public static PaymentCompletedEvent success(PointDeductedEvent event, long paymentId) {
        return of(event, paymentId, true);
    }

    // 실패한 결제를 위한 편의 메소드
    public static PaymentCompletedEvent failure(PointDeductedEvent event, long paymentId) {
        return of(event, paymentId, false);
    }

}

