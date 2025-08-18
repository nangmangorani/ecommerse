package kr.hhplus.be.server.eventHandler;

import jakarta.annotation.PreDestroy;
import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.domain.Payment;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;
    private final PointHistService pointHistService;
    private final ExecutorService stockDecreaseExecutor = Executors.newSingleThreadExecutor();

    @EventListener
    @Async("orderTaskExecutor")
    @Transactional
    public void handlePointDeduction(OrderCreatedEvent event) {
        try {
            userService.deductPointsWithLock(
                    event.getUserId(),
                    event.getRequestPrice()
            );
            orderEventPublisher.publishPointDeducted(
                    PointDeductedEvent.of(event)
            );

        } catch (Exception e) {
            productService.increaseStock(event.getProductId(), event.getRequestQuantity());
            orderService.cancelOrder(event.getOrderId());
        }
    }

    @EventListener
    @Async("orderTaskExecutor")
    @Transactional
    public void handlePayment(PointDeductedEvent event) {
        try {

            Payment result = paymentService.processPayment(
                    event.getOrderId(),
                    event.getRequestPrice()
            );

            if (result.getStatus().isCompleted()) {
                orderService.completeOrder(event.getOrderId());

                pointHistService.createPointHist(
                        userService.getUserInfo(event.getUserId(), UserStatus.ACTIVE),
                        TransactionType.USE,
                        event.getRequestPrice(),
                        userService.getUserInfo(event.getUserId(), UserStatus.ACTIVE).getPoint(),
                        result.getId()
                );

                orderEventPublisher.publishPaymentCompleted(
                        PaymentCompletedEvent.of(event, result.getId(),true)
                );
            } else {
                handlePaymentFailure(event);
            }

        } catch (Exception e) {
            throw new CustomException("결제실패");
        }
    }

    private void handlePaymentFailure(PointDeductedEvent event) {
        userService.refundPoints(event.getUserId(), event.getRequestPrice());

        productService.increaseStock(event.getProductId(), event.getRequestQuantity());

        orderService.cancelOrder(event.getOrderId());
    }

    @PreDestroy
    public void shutdown() {
        stockDecreaseExecutor.shutdown();
        try {
            if (!stockDecreaseExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                stockDecreaseExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            stockDecreaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
