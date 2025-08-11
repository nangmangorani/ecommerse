package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PointHistRepository;
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

    public OrderService(OrderRepository orderRepository, PointHistRepository pointHistRepository, UserRepository userRepository, ProductRepository productRepository, ProductService productService, UserService userService, PaymentService paymentService, CouponService couponService) {
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

        String status = "01";

        // 사용자 조회 및 잔고확인
        User user = userService.getUserAndCheckBalance(requestOrder);

        // 포인트 차감
        user.usePoint(requestOrder.requestPrice());

        // 주문시 재고확인
        Product product = productService.getProductInfo(requestOrder);

        // 상품 재고 차감
        product.decreaseStock(requestOrder.requestQuantity());

        // 쿠폰 조회
        Coupon coupon = couponService.searchCoupon(requestOrder.couponId());

        Order order = new Order(
                user,
                product,
                coupon,
                requestOrder.originalPrice(),
                requestOrder.requestPrice(),
                requestOrder.requestQuantity(),
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

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("주문을 찾을 수 없습니다"));
    }

    /**
     * 주문취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {

        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("주문을 찾을 수 없습니다"));

        // 2. 주문 상태를 취소로 변경
        order.cancel();

    }
}
