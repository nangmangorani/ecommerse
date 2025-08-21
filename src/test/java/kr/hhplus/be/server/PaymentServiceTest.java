package kr.hhplus.be.server;

import kr.hhplus.be.server.enums.*;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PaymentRepository;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.service.OrderService;
import kr.hhplus.be.server.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.*;

public class PaymentServiceTest {

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PointHistRepository pointHistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 결제 테스트
     * 1. 주문 후 포인트 차감 실패
     * 2. 주문 후 상품 재고 차감 실패
     * 3. 주문 후 결제내역 추가 실패
     */

    @Test
    @DisplayName("포인트 차감, 재고 차감, 결제 및 포인트이력 저장이 정상 작동")
    void 결제_정상적으로_처리() {

        User user = new User(1, "이승준", UserStatus.ACTIVE, 5000L);
        Product product = new Product(1, "상품1", ProductStatus.ACTIVE, 2, 10, 1000L, "필기구");
        PointHist pointHist = new PointHist(user, TransactionType.USE, 100L, 1000L,1);
        Payment payment = new Payment(1L, PaymentStatus.COMPLETED, 800, TransactionType.USE, LocalDateTime.now(),1L);
        Coupon coupon = new Coupon("쿠폰", CouponStatus.ACTIVE, 10,10,5,1);
        Order order = new Order(user, product, coupon, 1000L, 800L,1, OrderStatus.COMPLETED);

        RequestOrder requestOrder = new RequestOrder(
                user.getId(),
                product.getId(),
                1L,
                1,
                1000,
                800,
                true
        );

        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        given(pointHistRepository.save(any(PointHist.class))).willReturn(pointHist);

        paymentService.paymentProduct(requestOrder, user, product, order);

        assertEquals(4200L, user.getPoint());

    }
}
