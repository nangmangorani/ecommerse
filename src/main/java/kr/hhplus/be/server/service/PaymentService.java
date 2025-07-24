package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.TransactionType;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PaymentRepository;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PointRepository pointRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(PointRepository pointRepository, ProductRepository productRepository, OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.pointRepository = pointRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void paymentProduct(RequestOrder requestOrder, User user, Product product, Order order) {

        // 포인트 차감
        user.usePoint(requestOrder.requestPrice());

        // 상품 재고 차감
        product.decreaseStock(requestOrder.requestQuantity());

        // 결제이력 추가
        Payment payment = new Payment(
            "01",
                requestOrder.requestPrice(),
                TransactionType.USE,
                order.getId()
        );

        Payment returnPayment = paymentRepository.save(payment);

        // 포인트 이력 저장
        PointHist pointHist = new PointHist(
                TransactionType.CHARGE,
                returnPayment.getPrice(),
                user.getPoint()
        );

        PointHist returnPointHist = pointRepository.save(pointHist);

    }



}
