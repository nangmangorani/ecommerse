package kr.hhplus.be.server.enums;

public enum UserStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInactive() {
        return this == INACTIVE;
    }

}
