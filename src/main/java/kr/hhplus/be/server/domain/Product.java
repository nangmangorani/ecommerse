package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
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
        System.out.println("감소들어옴");
        if (quantity < amount) {
            System.out.println("감소들어옴 설마나?");
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

}
