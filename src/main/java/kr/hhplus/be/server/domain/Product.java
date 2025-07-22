package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

@Entity
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

    @Column(name = "PRICE", nullable = false)
    private int price;

    @Column(name = "PRODUCT_TYPE", length = 5)
    private String type;

    protected Product() {}

    public Product(String name, String status, int quantity, int price, String type) {
        this.name = name;
        this.status = status;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
    }

    public void decreaseStock(int amount) {
        if (quantity < amount) {
            throw new IllegalArgumentException("재고 부족");
        }
        this.quantity -= amount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getQuantity() { return quantity; }

}
