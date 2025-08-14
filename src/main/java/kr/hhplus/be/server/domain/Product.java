package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import lombok.Getter;

@Entity
@Getter
@Table(name = "PRODUCT")
public class
Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long id;

    @Column(name = "PRODUCT_NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "PRD_STATUS", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "PRODUCT_QUANTITY", nullable = false)
    private int quantity;

    @Column(name = "SELL_QUANTITY")
    private int sellQuantity;

    @Column(name = "PRICE", nullable = false)
    private long price;

    @Column(name = "PRODUCT_TYPE", length = 20)
    private String type;

    protected Product() {}

    public Product(long id, String name, ProductStatus status, int quantity, int sellQuantity,long price, String type) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.quantity = quantity;
        this.sellQuantity = sellQuantity;
        this.price = price;
        this.type = type;
    }

    public Product(String name, ProductStatus status, int quantity, int sellQuantity,long price, String type) {
        this.name = name;
        this.status = status;
        this.quantity = quantity;
        this.sellQuantity = sellQuantity;
        this.price = price;
        this.type = type;
    }

    public void decreaseStock(int amount) {
        if (quantity < amount) {
            throw new CustomException("요청수량보다 재고 부족");
        }
        this.quantity -= amount;
        this.sellQuantity += amount;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("증가할 수량은 0보다 커야 합니다.");
        }

        this.quantity += quantity;
    }

    // 상품원가검증과 할인가검증은 현재 하나의 메소드에서 처리
    public void checkPrice(long requestPrice, long productPrice) {
        if(requestPrice != productPrice) {
            throw new CustomException("요청하신 상품 금액이 다릅니다.");
        }
    }

    public void checkQuantity(long requestQuantity, long productQuantity) {

        if(productQuantity == 0) {
            throw new CustomException("상품 재고가 부족합니다.");
        }

        if(requestQuantity > productQuantity) {
            throw new CustomException("요청수량보다 상품 재고가 부족합니다.");
        }
    }

}
