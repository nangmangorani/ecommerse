package kr.hhplus.be.server.integrationTest;

import kr.hhplus.be.server.eventHandler.OrderEventHandler;
import kr.hhplus.be.server.eventHandler.OrderEventPublisher;
import kr.hhplus.be.server.eventHandler.OrderTransferData;
import kr.hhplus.be.server.eventHandler.PaymentCompletedEvent;
import kr.hhplus.be.server.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderDataTransferTest {

    @Mock
    private ProductService productService;

    @Mock
    private UserService userService;

    @Mock
    private PointService pointService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private PointHistService pointHistService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private OrderEventHandler orderEventHandler;

    private PaymentCompletedEvent testPaymentCompletedEvent;

    @BeforeEach
    void setUp() {
        testPaymentCompletedEvent = new PaymentCompletedEvent(
                1L, true, 1L, 1L, 1L, 1,
                10000L, 8000L, LocalDateTime.now(), "아이폰 15","홍길동"
        );
    }

    @Test
    @DisplayName("PaymentCompletedEvent에서 OrderTransferData로 올바르게 변환되어야 한다")
    void createOrderTransferData_ShouldConvertCorrectly() throws Exception {
        OrderTransferData result = (OrderTransferData) ReflectionTestUtils.invokeMethod(
                orderEventHandler,
                "createOrderTransferData",
                testPaymentCompletedEvent
        );

        assertAll("OrderTransferData 변환 검증",
                () -> assertThat(result.getUserName()).isEqualTo("홍길동"),
                () -> assertThat(result.getProductName()).isEqualTo("아이폰 15"),
                () -> assertThat(result.getQuantity()).isEqualTo(1),
                () -> assertThat(result.getOriginalPrice()).isEqualTo(10000L),
                () -> assertThat(result.getDiscountPrice()).isEqualTo(8000L),
                () -> assertThat(result.getCreatedAt()).isNotNull()
        );
    }

    @Test
    @DisplayName("여러 주문 정보를 연속으로 전송할 수 있어야 한다")
    void sendOrderInfo_ShouldHandleMultipleOrders() {
        PaymentCompletedEvent order1 = new PaymentCompletedEvent(
                100L, true, 1L, 1L, 1L, 1,
                10000L, 10000L, java.time.LocalDateTime.now(), "상품1", "사용자1"
        );
        PaymentCompletedEvent order2 = new PaymentCompletedEvent(
                200L, true, 2L, 2L, 2L, 2,
                25000L, 20000L, java.time.LocalDateTime.now(), "상품2", "사용자2"
        );

        assertAll("연속 처리",
                () -> orderEventHandler.sendOrderInfo(order1),
                () -> orderEventHandler.sendOrderInfo(order2)
        );
    }
}

