package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "COUPON_HIST")
@Getter
public class CouponHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_NO")
    private Long id;

    @Column(name = "COUPON_STATUS", length = 2, nullable = false)
    private String couponStatus; // 쿠폰 상태 01:사용가능 02:사용

    @Column(name = "ISSUED_DATETIME", nullable = false)
    private LocalDateTime issuedDateTime; // 발급 시간

    @Column(name = "USED_DATETIME")
    private LocalDateTime usedDateTime;   // 사용 시간

    @Column(name = "COUPON_ID")
    private Long couponId;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "USER_ID")
    private Long userId;

    protected CouponHist() {}

    public CouponHist(long couponId, long userId, long productId, String couponStatus) {
        this.couponId = couponId;
        this.userId = userId;
        this.productId = productId;
        this.couponStatus = couponStatus;
        this.issuedDateTime = LocalDateTime.now();
    }

    public void use() {
        this.couponStatus = "02";
        this.usedDateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }

}
