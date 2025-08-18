package kr.hhplus.be.server.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponHistStatus {
    ISSUED("발급"),
    USED("사용");

    private final String description;

    public boolean isIssued() {
        return this == ISSUED;
    }

    public boolean isUsed() {
        return this == USED;
    }
}
