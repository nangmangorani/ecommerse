package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.enums.OrderStatus;
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


    public OrderFacade(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher, UserService userService, CouponService couponService, ProductService productService) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.userService = userService;
        this.couponService = couponService;
        this.productService = productService;
    }

    @Transactional(timeout = 30)
    public ResponseOrder processOrder(RequestOrder request) {

        Order order = createOrderCore(request);

        orderEventPublisher.publishOrderCreated(OrderCreatedEvent.of(order));

        return ResponseOrder.from(order);
    }

    private Order createOrderCore(RequestOrder request) {

        User user = userService.getUserAndCheckBalance(request);

        Product product = productService.getProductInfo(request);

        productService.decreaseStockWithLock(request.productId(), request.requestQuantity());

        Coupon coupon = null;

        if (request.couponId() != null) {
            coupon = couponService.searchCouponByProductId(request.productId());
        }

        long expectedDiscountPrice = couponService.calculateDiscountedPrice(product, coupon);

        Order order = Order.create(user, product, coupon, expectedDiscountPrice, request.requestQuantity(), OrderStatus.IN_PROGRESS);

        return orderRepository.save(order);
    }

}
