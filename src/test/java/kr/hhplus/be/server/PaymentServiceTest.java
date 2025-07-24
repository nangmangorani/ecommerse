package kr.hhplus.be.server;

import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PaymentRepository;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.service.OrderService;
import kr.hhplus.be.server.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
    private PointRepository pointRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        // @Mock, @InjectMocks 어노테이션 필드 초기화
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

        User user = new User(1, "이승준", "Y", 5000L);  // 생성자 예시
        Product product = new Product(1, "상품1", "Y", 2, 10, 1000L, "필기구");  // 생성자 예시
        PointHist pointHist = new PointHist(TransactionType.USE, 100L, 1000L);
        Payment payment = new Payment("01", 800, TransactionType.USE, 1L);
        Order order = new Order(user, product, 1000L, 800L, "01");

        RequestOrder requestOrder = new RequestOrder(
                user.getId(),
                product.getId(),
                1,
                1,
                1000,
                800,
                "Y"
        );

        // given
        given(paymentRepository.save(any(Payment.class))).willReturn(payment);
        given(pointRepository.save(any(PointHist.class))).willReturn(pointHist);

        // when
        paymentService.paymentProduct(requestOrder, user, product, order);

        // then
        assertEquals(4200L, user.getPoint());

    }
}
