package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.TransactionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")

    private Long id;

    @Column(name = "PAYMENT_STATUS", length = 2, nullable = false)
    private String status;  // 결제 상태 01:결제완료 02:결제진행 03:결제취소

    @Column(name = "PAYMENT_PRICE", nullable = false)
    private long price;      // 결제 금액
    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TYPE", length = 20, nullable = false)
    private TransactionType type;    // 결제 수단

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "ORDER_NO", nullable = false)
    private Long orderNo;   // 주문 번호 (FK)

    protected Payment() {}

    public Payment(String status, long price, TransactionType type, Long orderNo) {
        this.status = status;
        this.price = price;
        this.type = type;
        this.orderNo = orderNo;
        this.paymentDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public long getPrice() { return price; }
    public String getStatus() { return status; }

}
