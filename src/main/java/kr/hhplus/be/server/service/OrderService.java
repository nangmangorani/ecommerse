package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Payment;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class OrderService {

    private final OrderRepository orderRepository;
    private final PointRepository pointRepository;
    private final ProductRepository productRepository;

    private PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, PointRepository pointRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.pointRepository = pointRepository;
        this.productRepository = productRepository;
    }

    /**
     * 상품주문
     * @return ResponseOrder
     */
    public ResponseOrder orderProduct(RequestOrder requestOrder) {

        // 사용자 및 포인트 조회
        User user = pointRepository.getPointById(requestOrder.userId())
                .orElseThrow(() -> new RuntimeException("사용자 미존재"));

        // 주문시 재고확인 (조회 후 개수가 0보다 작으면 오류)
        Product product = productRepository.findById(requestOrder.productId())
                .orElseThrow(() -> new RuntimeException("상품 미존재"));

        if(product.getQuantity() <= 0) {
            throw new RuntimeException("상품 재고 부족");
        }

        // 잔고 확인
        if(user.getPoint() < requestOrder.requestPrice()) {
            throw new RuntimeException("잔고 부족");
        }

        Order order = new Order(
                user,
                product,
                requestOrder.originalPrice(),
                requestOrder.requestPrice(),
                "02" // 결제진행
        );

        // 주문 추가
        Order returnOrder = orderRepository.save();

        // 재고 확인 후 결제
        paymentService.paymentProduct(requestOrder, user, product, returnOrder);


        return null;
    }
}
