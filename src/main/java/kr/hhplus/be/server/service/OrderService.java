package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.enums.OrderStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final CouponService couponService;

    /**
     * 상품주문
     * @return ResponseOrder
     */
    @Transactional
    public ResponseOrder orderProduct(RequestOrder requestOrder) {

        User user = userService.getUserAndCheckBalance(requestOrder);

        user.usePoint(requestOrder.requestPrice());

        Product product = productService.getProductInfo(requestOrder);

        product.decreaseStock(requestOrder.requestQuantity());

        Coupon coupon = couponService.searchCoupon(requestOrder.couponId());

        Order order = new Order(
                user,
                product,
                coupon,
                requestOrder.originalPrice(),
                requestOrder.requestPrice(),
                requestOrder.requestQuantity(),
                OrderStatus.IN_PROGRESS
        );
        Order returnOrder = orderRepository.save(order);

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

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException("주문을 찾을 수 없습니다"));

        order.cancel();

    }
}
