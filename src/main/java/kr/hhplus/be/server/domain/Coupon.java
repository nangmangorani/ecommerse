package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.exception.custom.CustomException;
import lombok.Getter;

@Entity
@Table(name = "COUPON")
@Getter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COUPON_ID")
    private Long id;

    @Column(name = "COUPON_NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "COUPON_STATUS", length = 2, nullable = false)
    private String status;

    @Column(name = "COUPON_TYPE", length = 2)
    private String type;

    @Column(name = "DISCOUNT_PERCENT", nullable = false)
    private int discountPercent;

    @Column(name = "MAX_QUANTITY")
    private int maxQuantity;

    @Column(name = "REMAIN_QUANTITY")
    private int remainQuantity;

    @Column(name = "PRODUCT_ID")
    private long productId;

    @Version
    private Long version = 0L;

    protected Coupon() {}

    public Coupon(String name, String status, int discountPercent, int maxQuantity, int remainQuantity, long productId) {
        this.name = name;
        this.status = status;
        this.discountPercent = discountPercent;
        this.maxQuantity = maxQuantity;
        this.remainQuantity = remainQuantity;
        this.productId = productId;
    }

    public void issueCoupon() {
        if (remainQuantity <= 0) throw new CustomException("쿠폰 소진");
        this.remainQuantity -= 1;
    }

}
