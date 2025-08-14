package kr.hhplus.be.server.enums;

public enum OrderStatus {

    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
