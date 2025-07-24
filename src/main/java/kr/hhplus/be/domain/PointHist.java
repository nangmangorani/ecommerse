package kr.hhplus.be.domain;

import jakarta.persistence.*;
import kr.hhplus.be.TransactionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "POINT_HIST")
public class PointHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POINT_HIST_ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TRANSACTION_TYPE", length = 20, nullable = false)
    private TransactionType transactionType;

    @Column(name = "AMOUNT", nullable = false)
    private long amount;

    @Column(name = "CURRENT_BALANCE", nullable = false)
    private long currentBalance;

    @Column(name = "TRANSACTION_DATETIME", nullable = false)
    private LocalDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "PAYMENT_ID")
    private Long paymentNo;

    protected PointHist() {}

    public PointHist(TransactionType transactionType, long amount, long currentBalance) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.currentBalance = currentBalance;
        this.transactionDateTime = LocalDateTime.now();
    }

}
