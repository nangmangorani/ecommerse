package kr.hhplus.be.server.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseUserCouponDto {

    private long userId;
    private long couponId;
    private String userName;
    private String couponName;
    private double discountPercent;

}
