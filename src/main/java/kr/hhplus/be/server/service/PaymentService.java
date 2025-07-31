package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.TransactionType;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.order.ResponseOrder;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.repository.OrderRepository;
import kr.hhplus.be.server.repository.PaymentRepository;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.domain.*;
import org.springframework.stereotype.Service;

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

    /**
     * 상품결제
     * @param requestOrder
     * @param user
     * @param product
     * @param order
     */

    public void paymentProduct(RequestOrder requestOrder, User user, Product product, Order order) {

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
                user,
                TransactionType.USE,
                returnPayment.getPrice(),
                user.getPoint(),
                returnPayment.getId()
        );

        PointHist returnPointHist = pointRepository.save(pointHist);

    }

    public User chargePoint(User user, RequestPointCharge requestPointCharge) {

        // 포인트 충전
        user.addPoint(requestPointCharge.userPoint());

        // 결제이력 추가
        Payment payment = new Payment(
                "01",
                requestPointCharge.userPoint(),
                TransactionType.CHARGE
        );

        Payment returnPayment = paymentRepository.save(payment);

        // 포인트 이력 저장
        PointHist pointHist = new PointHist(
                user,
                TransactionType.CHARGE,
                returnPayment.getPrice(),
                user.getPoint(),
                returnPayment.getId()
        );

        PointHist returnPointHist = pointRepository.save(pointHist);

        return user;
    }



}
