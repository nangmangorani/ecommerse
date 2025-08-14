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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final UserService userService;
    private final CouponService couponService;
    private final ProductService productService;
    private final RedissonClient redissonClient;


    public OrderFacade(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher, UserService userService, CouponService couponService, ProductService productService, RedissonClient redissonClient) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.userService = userService;
        this.couponService = couponService;
        this.productService = productService;
        this.redissonClient = redissonClient;
    }

    public ResponseOrder processOrder(RequestOrder request) {
        String lockKey = "product:stock:" + request.productId();
        RLock lock = redissonClient.getLock(lockKey);

        int maxRetry = 5;
        int retryDelay = 50;

        try {
            boolean locked = false;
            for (int i = 0; i < maxRetry; i++) {
                if (lock.tryLock(5, 5, TimeUnit.SECONDS)) {
                    locked = true;
                    break;
                }
                Thread.sleep(retryDelay);
            }

            if (!locked) {
                throw new CustomException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            Order order = createOrderCore(request);

            orderEventPublisher.publishOrderCreated(OrderCreatedEvent.of(order));

            return ResponseOrder.from(order);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException("주문 처리 중 오류가 발생했습니다.");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(timeout = 10)
    private Order createOrderCore(RequestOrder request) {

        User user = userService.getUserAndCheckBalance(request);

        Product product = productService.getProductInfo(request);

        productService.decreaseStock(request.productId(), request.requestQuantity());

        Coupon coupon = null;

        if (request.couponId() != null) {
            coupon = couponService.searchCouponByProductId(request.productId());
        }

        long expectedDiscountPrice = couponService.calculateDiscountedPrice(product, coupon);

        Order order = Order.create(user, product, coupon, expectedDiscountPrice, request.requestQuantity(), OrderStatus.IN_PROGRESS);

        return orderRepository.save(order);
    }

}
