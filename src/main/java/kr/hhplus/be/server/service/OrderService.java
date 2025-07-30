package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final CouponService couponService;

    public OrderService(OrderRepository orderRepository, PointRepository pointRepository, UserRepository userRepository, ProductRepository productRepository, ProductService productService, UserService userService, PaymentService paymentService, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.couponService = couponService;
    }

    /**
     * 상품주문
     * @return ResponseOrder
     */
    @Transactional
    public ResponseOrder orderProduct(RequestOrder requestOrder) {

        // 사용자 조회 및 잔고확인
        User user = userService.getUserAndCheckBalance(requestOrder.userId(), requestOrder.requestPrice());

        // 주문시 재고확인
        Product product = productService.getProductInfo(requestOrder.productId());

        // 쿠폰 조회
        Coupon coupon = couponService.searchCoupon(requestOrder.couponId());

        Order order = new Order(
                user,
                product,
                requestOrder.originalPrice(),
                requestOrder.requestPrice(),
                "02" // 결제진행
        );

        // 주문 추가
        Order returnOrder = orderRepository.save(order);

        // 재고 확인 후 결제
        paymentService.paymentProduct(requestOrder, user, product, returnOrder);

        ResponseOrder responseOrder = new ResponseOrder(
                user.getName(),
                product.getName(),
                coupon.getName(),
                requestOrder.couponYn(),
                requestOrder.originalPrice(),
                requestOrder.requestPrice()
        );

        return responseOrder;
    }
}
