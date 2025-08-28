package kr.hhplus.be.server.eventHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCreated(OrderCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void publishPointDeducted(PointDeductedEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        eventPublisher.publishEvent(event);
    }

}
