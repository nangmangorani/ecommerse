package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "COUPON_HIST")
public class CouponHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_NO")
    private Long id;

    @Column(name = "COUPON_STATUS", length = 2, nullable = false)
    private String couponStatus; // 쿠폰 상태

    @Column(name = "ISSUED_DATETIME", nullable = false)
    private LocalDateTime issuedDateTime; // 발급 시간

    @Column(name = "USED_DATETIME")
    private LocalDateTime usedDateTime;   // 사용 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPON_ID", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    protected CouponHist() {}

    public CouponHist(Coupon coupon, Product product, User user, String couponStatus) {
        this.coupon = coupon;
        this.product = product;
        this.user = user;
        this.couponStatus = couponStatus;
        this.issuedDateTime = LocalDateTime.now();
    }

    public void use() {
        this.couponStatus = "USED";
        this.usedDateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }

}
