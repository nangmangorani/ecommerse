package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.enums.OrderStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ORDERS")
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_NO")
    private Long id;

    @Column(name = "ORDER_STATUS", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "ORIGINAL_PRICE", nullable = false)
    private long originalPrice;

    @Column(name = "DISCOUNTED_PRICE")
    private long discountedPrice;

    @Column(name = "REQUEST_QUANTITY")
    private int requestQuantity;

    @Column(name = "ORDER_DATETIME", nullable = false)
    private LocalDateTime orderDateTime;

    @Column(name = "SOLD_DATETIME")
    private LocalDateTime soldDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false
            ,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false
            ,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPON_ID", nullable = true
            ,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Coupon coupon;

    protected Order() {}

    public Order(User user, Product product, Coupon coupon, long originalPrice, long discountedPrice, int requestQuantity ,OrderStatus status) {
        this.user = user;
        this.product = product;
        this.coupon = coupon;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.requestQuantity = requestQuantity;
        this.status = status;
        this.orderDateTime = LocalDateTime.now();
    }

    public static Order create(User user, Product product, Coupon coupon, long discountedPrice, int requestQuantity, OrderStatus orderStatus) {
        return new Order(user, product, coupon, product.getPrice(), discountedPrice, requestQuantity, orderStatus);
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new CustomException("이미 취소된 주문입니다.");
        }

        this.status = OrderStatus.CANCELLED;
    }

}
