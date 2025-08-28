package kr.hhplus.be.server.integrationTest;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.enums.OrderStatus;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.eventHandler.OrderCreatedEvent;
import kr.hhplus.be.server.eventHandler.OrderEventPublisher;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.service.CouponService;
import kr.hhplus.be.server.service.OrderFacade;
import kr.hhplus.be.server.service.ProductService;
import kr.hhplus.be.server.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class OrderEventTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private UserService userService;

    @Mock
    private CouponService couponService;

    @Mock
    private ProductService productService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private OrderFacade orderFacade;

    private User testUser;
    private Product testProduct;
    private Coupon testCoupon;
    private Order testOrder;
    private RequestOrder testRequest;
    @BeforeEach
    void setUp() throws InterruptedException {
        testUser = new User(1L, "테스트유저", UserStatus.ACTIVE, 100000L);

        testProduct = new Product(1L, "테스트상품", ProductStatus.ACTIVE, 10, 0, 20000L, "전자제품");

        testCoupon = new Coupon("테스트쿠폰", CouponStatus.ACTIVE, 20, 10, 5, 1L);

        testOrder = Order.create(testUser, testProduct, testCoupon, 16000L, 2, OrderStatus.IN_PROGRESS);

        testRequest = new RequestOrder(1L, 1L, 1L, 2, 40000L, 32000L, true);

        when(redissonClient.getLock(anyString())).thenReturn(lock);

    }

    @Test
    @DisplayName("주문 처리 성공 시 OrderCreatedEvent가 정상적으로 발행되어야 한다")
    void 주문처리_성공시_이벤트_정상발행() throws InterruptedException {

        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(userService.getUserAndCheckBalance(testRequest)).thenReturn(testUser);
        when(productService.getProductInfo(testRequest)).thenReturn(testProduct);
        when(couponService.searchCouponByProductId(1L)).thenReturn(testCoupon);
        when(couponService.calculateDiscountedPrice(testProduct, testCoupon)).thenReturn(16000L);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        ResponseOrder result = orderFacade.processOrder(testRequest);

        verify(orderEventPublisher, times(1)).publishOrderCreated(any(OrderCreatedEvent.class));

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publishOrderCreated(eventCaptor.capture());

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();

        assertAll("OrderCreatedEvent 검증",
                () -> assertThat(publishedEvent.getOrderId()).isEqualTo(testOrder.getId()),
                () -> assertThat(publishedEvent.getUserId()).isEqualTo(1L),
                () -> assertThat(publishedEvent.getProductId()).isEqualTo(1L),
                () -> assertThat(publishedEvent.getRequestQuantity()).isEqualTo(2),
                () -> assertThat(publishedEvent.getRequestPrice()).isEqualTo(16000L)
        );

        assertAll("ResponseOrder 검증",
                () -> assertThat(result.userName()).isEqualTo("테스트유저"),
                () -> assertThat(result.productName()).isEqualTo("테스트상품"),
                () -> assertThat(result.couponYn()).isTrue()
        );

    }

    @Test
    @DisplayName("Redis Lock 획득 실패 시 이벤트가 발행되지 않아야 한다")
    void 락획득_실패시_이벤트_발행안됨() throws InterruptedException {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        try {
            orderFacade.processOrder(testRequest);
        } catch (Exception e) {
        }

        verify(orderEventPublisher, never()).publishOrderCreated(any(OrderCreatedEvent.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 과정에서 예외 발생 시 이벤트가 발행되지 않아야 한다")
    void 주문생성중_예외발생_이벤트발행안됨() throws InterruptedException {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(userService.getUserAndCheckBalance(testRequest)).thenThrow(new RuntimeException("잔고 부족"));

        try {
            orderFacade.processOrder(testRequest);
        } catch (Exception e) {
        }

        verify(orderEventPublisher, never()).publishOrderCreated(any(OrderCreatedEvent.class));
    }

}
