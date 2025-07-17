package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.coupon.RequestUserCouponDto;
import kr.hhplus.be.server.dto.coupon.ResponseUserCouponDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupon")
@Tag(name = "쿠폰 API", description = "쿠폰발급")
public class CouponController {

    @Operation(summary = "선착순 쿠폰발급")
    @PostMapping("/fcfs")
    public ResponseEntity<ResponseUserCouponDto> issueCoupon(@RequestBody RequestUserCouponDto requestUserCouponDto) {

        ResponseUserCouponDto returnUserCouponDto = new ResponseUserCouponDto(1,1,"이승준","의류 20% 할인쿠폰",20.0);

        return ResponseEntity.ok(returnUserCouponDto);
    }

}
