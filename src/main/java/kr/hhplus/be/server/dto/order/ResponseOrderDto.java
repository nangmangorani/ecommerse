package kr.hhplus.be.server.dto.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseOrderDto {

    private long userId;
    private long productId;
    private long couponId;
    private String userName;
    private String productName;
    private String couponName;
    private String couponYn;
    private int originalPrice;
    private int discountPrice;


}
