package kr.hhplus.be.server.eventHandler;

import jakarta.annotation.PreDestroy;
import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.domain.Payment;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.service.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class OrderEventHandler {

    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;
    private final PointHistService pointHistService;

    // 재고 차감만을 전담하는 단일 스레드 Executor를 생성합니다.
    private final ExecutorService stockDecreaseExecutor = Executors.newSingleThreadExecutor();


    public OrderEventHandler(ProductService productService, UserService userService, PaymentService paymentService, OrderService orderService, OrderEventPublisher orderEventPublisher, PointHistService pointHistService) {
            this.productService = productService;
            this.userService = userService;
            this.paymentService = paymentService;
            this.orderService = orderService;
            this.orderEventPublisher = orderEventPublisher;
            this.pointHistService = pointHistService;
        }

    @EventListener
    @Async("orderTaskExecutor")
    @Transactional
    public void handlePointDeduction(OrderCreatedEvent event) {
        try {
            // 실제 포인트 차감 (동시성 제어 적용)
            userService.deductPointsWithLock(
                    event.getUserId(),
                    event.getRequestPrice()
            );
            // 다음 단계 이벤트 발행
            orderEventPublisher.publishPointDeducted(
                    PointDeductedEvent.of(event)
            );

        } catch (Exception e) {
            // 포인트 부족 시 재고 롤백 및 주문 취소
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
                // 주문 완료 처리
                orderService.completeOrder(event.getOrderId());

                pointHistService.createPointHist(
                        userService.getUserInfo(event.getUserId(), UserStatus.ACTIVE),
                        TransactionType.USE,
                        event.getRequestPrice(),
                        userService.getUserInfo(event.getUserId(), UserStatus.ACTIVE).getPoint(),
                        result.getId()  // 결제 ID
                );

                orderEventPublisher.publishPaymentCompleted(
                        PaymentCompletedEvent.of(event, result.getId(),true)
                );
            } else {
                // 결제 실패 시 보상 트랜잭션
                handlePaymentFailure(event);
            }

        } catch (Exception e) {
            throw new CustomException("결제실패");
        }
    }

    private void handlePaymentFailure(PointDeductedEvent event) {
        // 포인트 복구
        userService.refundPoints(event.getUserId(), event.getRequestPrice());
        // 재고 복구
        productService.increaseStock(event.getProductId(), event.getRequestQuantity());
        // 주문 취소
        orderService.cancelOrder(event.getOrderId());
    }

    // 애플리케이션 종료 시 스레드 풀을 안전하게 종료합니다.
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
