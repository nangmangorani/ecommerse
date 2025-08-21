package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.enums.CouponHistStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "COUPON_HIST")
@Getter
@Builder
@AllArgsConstructor
public class CouponHist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ_NO")
    private Long id;

    @Column(name = "COUPON_STATUS", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponHistStatus couponStatus;

    @Column(name = "ISSUED_DATETIME", nullable = false)
    private LocalDateTime issuedDateTime;

    @Column(name = "USED_DATETIME")
    private LocalDateTime usedDateTime;

    @Column(name = "COUPON_ID")
    private Long couponId;

    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "USER_ID")
    private Long userId;

    protected CouponHist() {}

    public CouponHist(long couponId, long userId, long productId, CouponHistStatus couponStatus) {
        this.couponId = couponId;
        this.userId = userId;
        this.productId = productId;
        this.couponStatus = couponStatus;
        this.issuedDateTime = LocalDateTime.now();
    }

    public void use() {
        this.couponStatus = CouponHistStatus.USED;
        this.usedDateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }

}
