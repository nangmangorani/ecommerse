package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_NO")
    private Long id;

    @Column(name = "ORDER_STATUS", length = 2, nullable = false)
    private String status; // 01:결제완료 02:결제진행 03:결제취소

    @Column(name = "ORIGINAL_PRICE", nullable = false)
    private long originalPrice;

    @Column(name = "DISCOUNTED_PRICE")
    private long discountedPrice;

    @Column(name = "ORDER_DATETIME", nullable = false)
    private LocalDateTime orderDateTime;

    @Column(name = "SOLD_DATETIME")
    private LocalDateTime soldDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    protected Order() {}

    public Order(User user, Product product, long originalPrice, long discountedPrice, String status) {
        this.user = user;
        this.product = product;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.status = status;
        this.orderDateTime = LocalDateTime.now();
    }

    public static Order create(User user, Product product, int discountedPrice) {
        return new Order(user, product, product.getPrice(), discountedPrice, "01");
    }

    public Long getId() { return id; }

}
