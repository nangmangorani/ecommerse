package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_HIST")
public class PointHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POINT_HIST_ID")
    private Long id;

    @Column(name = "TRANSACTION_TYPE", length = 20, nullable = false)
    private String transactionType;

    @Column(name = "AMOUNT", nullable = false)
    private int amount;

    @Column(name = "CURRENT_BALANCE", nullable = false)
    private int currentBalance;

    @Column(name = "TRANSACTION_DATETIME", nullable = false)
    private LocalDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "PAYMENT_ID")
    private Long paymentNo;

    protected PointHist() {}

    public PointHist(User user, String transactionType, int amount, int currentBalance) {
        this.user = user;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currentBalance = currentBalance;
        this.transactionDateTime = LocalDateTime.now();
    }

}
