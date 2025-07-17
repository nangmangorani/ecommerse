package kr.hhplus.be.server.dto.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RequestUserCouponDto {

    private long userId;
    private long couponId;

}
