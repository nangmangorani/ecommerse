package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.enums.PaymentStatus;
import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.repository.PaymentRepository;
import kr.hhplus.be.server.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PointHistService pointHistService;
    private final PaymentRepository paymentRepository;

    /**
     * 상품결제
     * @param requestOrder
     * @param user
     * @param product
     * @param order
     */

    public void paymentProduct(RequestOrder requestOrder, User user, Product product, Order order) {

        Payment payment = new Payment(
            PaymentStatus.COMPLETED,
                requestOrder.requestPrice(),
                TransactionType.USE,
                order.getId()
        );

        Payment returnPayment = paymentRepository.save(payment);

        pointHistService.createPointHist(user,
                TransactionType.USE,
                returnPayment.getPrice(),
                user.getPoint(),
                returnPayment.getId());
    }

    @Transactional
    public Payment processPayment(long orderId, long amount) {
        try {
            Payment payment = Payment.create(PaymentStatus.COMPLETED, amount, TransactionType.USE, orderId);
            return paymentRepository.save(payment);
        } catch (Exception e) {
            Payment failedPayment = Payment.create(PaymentStatus.CANCELLED, amount, TransactionType.USE, orderId);
            return paymentRepository.save(failedPayment);
        }
    }


    public User chargePoint(User user, RequestPointCharge requestPointCharge) {

        user.addPoint(requestPointCharge.userPoint());

        Payment payment = new Payment(
                PaymentStatus.COMPLETED,
                requestPointCharge.userPoint(),
                TransactionType.CHARGE
        );

        Payment returnPayment = paymentRepository.save(payment);

        pointHistService.createPointHist(user,
                TransactionType.CHARGE,
                returnPayment.getPrice(),
                user.getPoint(),
                returnPayment.getId());

        return user;
    }



}
