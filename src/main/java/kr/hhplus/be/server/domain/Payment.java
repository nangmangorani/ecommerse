package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.enums.PaymentStatus;
import kr.hhplus.be.server.enums.TransactionType;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")

    private Long id;

    @Column(name = "PAYMENT_STATUS", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "PAYMENT_PRICE", nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TYPE", length = 20, nullable = false)
    private TransactionType type;

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "ORDER_NO")
    private Long orderNo;

    protected Payment() {}

    public Payment(PaymentStatus status, long price, TransactionType type, Long orderNo) {
        this.status = status;
        this.price = price;
        this.type = type;
        this.orderNo = orderNo;
        this.paymentDate = LocalDateTime.now();
    }

    public Payment(PaymentStatus status, long price, TransactionType type) {
        this.status = status;
        this.price = price;
        this.type = type;
        this.paymentDate = LocalDateTime.now();
    }

    public static Payment create(PaymentStatus status, long price, TransactionType type, Long orderNo) {
        return new Payment(status, price, type, orderNo);
    }




}
