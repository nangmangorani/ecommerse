package kr.hhplus.be.server;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

public class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PointHistRepository pointHistRepository;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // @Mock, @InjectMocks 어노테이션 필드 초기화
        MockitoAnnotations.openMocks(this);
    }


    /**
     * 주문 및 결제 테스트
     * 1. 주문시 주문자가 조회되지 않을 때
     * 2. 주문시 상품이 존재하지 않을 때
     * 3. 주문시 상품 재고가 부족할 때
     * 4. 주문은 되었지만 잔고가 부족할 때
     */

    @Test
    @DisplayName("주문 시 주문자가 조회되지 않을 때")
    void 주문시_주문자가_조회되지_않을때() {

        RequestOrder requestOrder = new RequestOrder(1L, 1L, 1L, 1,1000L, 100,true);

        // given
        given(userRepository.findById(requestOrder.userId())).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> orderService.orderProduct(requestOrder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자 미존재");
    }

    @Test
    @DisplayName("주문시 상품이 존재하지 않을 때")
    void 주문시_상품이_존재하지_않을때() {

        RequestOrder requestOrder = new RequestOrder(1L, 1L, 1L, 1,1000L, 100, true);

        User user = new User(1,"이승준", "Y", 1000L);

        // given
        given(userRepository.findById(requestOrder.userId())).willReturn(Optional.of(user));
        given(productRepository.findById(requestOrder.productId())).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> orderService.orderProduct(requestOrder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품 미존재");
    }

    @Test
    @DisplayName("주문시 상품 재고가 부족할 때")
    void 주문시_상품재고가_부족할때() {

        RequestOrder requestOrder = new RequestOrder(1L, 1L, 1L, 1,1000L, 100, true);
        User user = new User(1,"이승준", "Y", 1000L);
        Product product = new Product(1,"상품1","Y",0,10,1000L,"필기구");

        // given
        given(userRepository.findById(requestOrder.userId())).willReturn(Optional.of(user));
        given(productRepository.findById(requestOrder.productId())).willReturn(Optional.of(product));

        // when, then
        assertThatThrownBy(() -> orderService.orderProduct(requestOrder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품 재고 부족");
    }

    @Test
    @DisplayName("주문은 되었지만 잔고가 부족할 때")
    void 주문시_잔고가_부족할때() {

        RequestOrder requestOrder = new RequestOrder(1L, 1L, 1L, 1,11000L, 10000, true);
        User user = new User(1,"이승준", "Y", 1000L);
        Product product = new Product(1,"상품1","Y",1,10,1000L,"필기구");

        // given
        given(userRepository.findById(requestOrder.userId())).willReturn(Optional.of(user));
        given(productRepository.findById(requestOrder.productId())).willReturn(Optional.of(product));

        // when, then
        assertThatThrownBy(() -> orderService.orderProduct(requestOrder))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("잔고 부족");

    }



}
