package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.exception.custom.CustomException;
import lombok.Getter;

@Entity
@Getter
@Table(name = "PRODUCT")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long id;

    @Column(name = "PRODUCT_NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "PRD_STATUS", length = 7, nullable = false)
    private String status;

    @Column(name = "PRODUCT_QUANTITY", nullable = false)
    private int quantity;

    @Column(name = "SELL_QUANTITY")
    private int sellQuantity;

    @Column(name = "PRICE", nullable = false)
    private long price;

    @Column(name = "PRODUCT_TYPE", length = 5)
    private String type;

    protected Product() {}

    public Product(long id, String name, String status, int quantity, int sellQuantity,long price, String type) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.quantity = quantity;
        this.sellQuantity = sellQuantity;
        this.price = price;
        this.type = type;
    }

    public Product(String name, String status, int quantity, int sellQuantity,long price, String type) {
        this.name = name;
        this.status = status;
        this.quantity = quantity;
        this.sellQuantity = sellQuantity;
        this.price = price;
        this.type = type;
    }

    public void decreaseStock(int amount) {
        if (quantity < amount) {
            throw new CustomException("재고 부족");
        }
        this.quantity -= amount;
        this.sellQuantity += amount;
    }

}
