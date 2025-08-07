package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.eventHandler.OrderCreatedEvent;
import kr.hhplus.be.server.eventHandler.OrderEventPublisher;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.CouponRepository;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final UserService userService;
    private final CouponService couponService;
    private final ProductService productService;
    private final ProductRepository productRepository;


    public OrderFacade(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository, OrderEventPublisher orderEventPublisher, CouponRepository couponRepository, UserService userService, CouponService couponService, ProductService productService) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.userService = userService;
        this.couponService = couponService;
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Transactional(timeout = 30)
    public ResponseOrder processOrder(RequestOrder request) {

        // 1. 핵심 주문 생성만 트랜잭션으로 처리
        Order order = createOrderCore(request);

        System.out.println("비동기처리전!@!@@");

        // 2. 비동기 후속 처리 이벤트 발행
        orderEventPublisher.publishOrderCreated(OrderCreatedEvent.of(order));

        return ResponseOrder.from(order);
    }

    private Order createOrderCore(RequestOrder request) {

        String status = "01";

        User user = userService.getUserAndCheckBalance(request.userId(), request.requestPrice(), status);

        productService.decreaseStockWithLock(request.productId(), request.requestQuantity());

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new CustomException("상품이 존재하지 않습니다."));

        // 요청한 상품원금과 실제 상품 가격 검증
        if (request.originalPrice() != product.getPrice()) {
            throw new CustomException("상품 가격이 일치하지 않습니다.");
        }

        Coupon coupon = null;

        if (request.couponId() != null) {
            coupon = couponService.searchCouponByProductId(request.productId());
        }

        // 쿠폰 할인금액과 사용자 요청금액 검증
        long expectedDiscountPrice;

        if (coupon != null) {
            expectedDiscountPrice = product.getPrice() - (product.getPrice() * coupon.getDiscountPercent() / 100);
        } else {
            expectedDiscountPrice = product.getPrice();
        }

        if (user.getPoint() < request.requestPrice()) {
            throw new CustomException("잔고부족");
        }

        Order order = Order.create(user, product, coupon, request.requestPrice(), request.requestQuantity());

        return orderRepository.save(order);
    }

}
