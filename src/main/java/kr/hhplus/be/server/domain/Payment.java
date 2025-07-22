package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYMENT_ID")

    private Long id;

    @Column(name = "PAYMENT_STATUS", length = 2, nullable = false)
    private String status;  // 결제 상태

    @Column(name = "PAYMENT_PRICE", nullable = false)
    private int price;      // 결제 금액

    @Column(name = "PAYMENT_TYPE", length = 20, nullable = false)
    private String type;    // 결제 수단

    @Column(name = "PAYMENT_DATE", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "ORDER_NO", nullable = false)
    private Long orderNo;   // 주문 번호 (FK)

    protected Payment() {}

    public Payment(String status, int price, String type, Long orderNo) {
        this.status = status;
        this.price = price;
        this.type = type;
        this.orderNo = orderNo;
        this.paymentDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public int getPrice() { return price; }
    public String getStatus() { return status; }

}
