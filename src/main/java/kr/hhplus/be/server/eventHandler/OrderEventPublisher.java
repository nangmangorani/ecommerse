package kr.hhplus.be.server.eventHandler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrderEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

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
