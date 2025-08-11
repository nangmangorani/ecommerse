package kr.hhplus.be.server.enums;

public enum ProductStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    OUT_OF_STOCK("품절");

    private final String description;

    ProductStatus(String description) {
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

    public boolean isOutOfStock() {
        return this == OUT_OF_STOCK;
    }
}
