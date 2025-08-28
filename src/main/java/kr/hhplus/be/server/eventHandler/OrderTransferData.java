package kr.hhplus.be.server.eventHandler;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderTransferData {
    private final String userName;
    private final String productName;
    private final int quantity;
    private final long originalPrice;
    private final long discountPrice;
    private final LocalDateTime createdAt;
}
