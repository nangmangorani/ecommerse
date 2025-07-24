package kr.hhplus.be.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    @Column(name = "USER_NAME", length = 10, nullable = false)
    private String name;

    @Column(name = "USER_STATUS", length = 2, nullable = false)
    private String status;

    @Column(name = "USER_POINT", nullable = false)
    private Long point;

    @Column(name = "REG_DATETIME", nullable = false)
    private LocalDateTime registeredAt;

    protected User() { }

    public User(long id, String name, String status, Long point) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.point = point;
        this.registeredAt = LocalDateTime.now();
    }

    public void addPoint(long amount) {
        this.point += amount;
    }

    public void usePoint(long amount) {
        if (this.point < amount) {
            throw new IllegalArgumentException("포인트 부족");
        }
        this.point -= amount;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getPoint() { return point; }

}
