package kr.hhplus.be.server.eventHandler;

import jakarta.annotation.PreDestroy;
import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.domain.Payment;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    private final ProductService productService;
    private final UserService userService;
    private final PointService pointService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;
    private final PointHistService pointHistService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ExecutorService stockDecreaseExecutor = Executors.newSingleThreadExecutor();

    @EventListener
    @Async("orderTaskExecutor")
    @Transactional
    public void handlePointDeduction(OrderCreatedEvent event) {
        try {
            pointService.deductPointsWithLock(
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

                updatePopularityScore(event.getProductId(), event.getRequestQuantity());

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

    @EventListener
    @Async("orderTaskExecutor")
    public void sendOrderInfo(PaymentCompletedEvent event) {

        try {
            log.info("주문정보 전송 시작 - 주문ID: {}", event.getOrderId());

            OrderTransferData orderData = createOrderTransferData(event);

            sendOrderData(orderData);
            log.info("주문정보 전송 완료 - 주문ID: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("주문정보 전송 실패 - 주문ID: {}", event.getOrderId(), e);
        }

    }

    private void updatePopularityScore(Long productId, int quantity) {
        try {
            String dailyKey = getDailyPopularKey();
            String weeklyKey = getWeeklyPopularKey();

            redisTemplate.opsForZSet().incrementScore(dailyKey, productId.toString(), quantity);
            redisTemplate.opsForZSet().incrementScore(weeklyKey, productId.toString(), quantity);

            Boolean dailyExpire = redisTemplate.expire(dailyKey, Duration.ofDays(1));
            Boolean weeklyExpire = redisTemplate.expire(weeklyKey, Duration.ofDays(7));

            if (!Boolean.TRUE.equals(dailyExpire)) {
                log.warn("일간 인기상품 TTL 설정 실패 - key: {}", dailyKey);
            }

            if (!Boolean.TRUE.equals(weeklyExpire)) {
                log.warn("주간 인기상품 TTL 설정 실패 - key: {}", weeklyKey);
            }

            log.info("상품 {} 인기도 점수 {}점 증가", productId, quantity);

        } catch (Exception e) {
            log.error("인기상품 점수 업데이트 실패 - 상품 ID: {}, 수량: {}", productId, quantity, e);
        }
    }

    private void handlePaymentFailure(PointDeductedEvent event) {
        pointService.refundPoints(event.getUserId(), event.getRequestPrice());

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

    private String getDailyPopularKey() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "popular:daily:" + today;
    }

    private String getWeeklyPopularKey() {
        int weekOfYear = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        return "popular:weekly:" + LocalDate.now().getYear() + ":" + weekOfYear;
    }

    private void sendOrderData(OrderTransferData orderTransferData) {

        try {
            String jsonData = convertToJson(orderTransferData);

            log.info("데이터 전송 완료 - 데이터: {}", jsonData);

        } catch (CustomException e) {
            Thread.currentThread().interrupt();
            throw new CustomException("전송 중 인터럽트 발생");
        }

        log.info("사용자 {}에게 주문 {} 정보를 전송", orderTransferData.getUserName(), orderTransferData.getProductName());
    }

    private OrderTransferData createOrderTransferData(PaymentCompletedEvent event) {
        return OrderTransferData.builder()
                .userName(event.getUserName())
                .productName(event.getProductName())
                .quantity(event.getRequestQuantity())
                .originalPrice(event.getOriginalPrice())
                .discountPrice(event.getRequestPrice())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String convertToJson(OrderTransferData orderData) {
        return String.format(
                "{\"userName\":\"%s\",\"productName\":\"%s\",\"quantity\":%d,\"originalPrice\":%d,\"discountPrice\":%d,\"createdAt\":\"%s\"}",
                orderData.getUserName(),
                orderData.getProductName(),
                orderData.getQuantity(),
                orderData.getOriginalPrice(),
                orderData.getDiscountPrice(),
                orderData.getCreatedAt()
        );
    }

}
