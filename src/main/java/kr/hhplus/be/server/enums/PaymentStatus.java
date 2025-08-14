package kr.hhplus.be.server.enums;

public enum PaymentStatus {
    IN_PROGRESS("결제진행"),
    COMPLETED("결제완료"),
    CANCELLED("결제취소");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }
}
