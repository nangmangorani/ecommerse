package kr.hhplus.be.server.enums;

public enum CouponHistStatus {
    ISSUED("발급"),
    USED("사용");

    private final String description;

    CouponHistStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // 상태 확인 메소드들
    public boolean isIssued() {
        return this == ISSUED;
    }

    public boolean isUsed() {
        return this == USED;
    }
}
