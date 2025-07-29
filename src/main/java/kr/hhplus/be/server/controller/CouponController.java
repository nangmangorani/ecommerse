package kr.hhplus.be.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.dto.coupon.ResponseUserCoupon;
import kr.hhplus.be.server.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/coupons")
@Tag(name = "쿠폰 API", description = "쿠폰발급")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "선착순 쿠폰발급")
    @PostMapping("/issue")
    public ResponseEntity<ResponseUserCoupon> issueCoupon(@RequestBody RequestUserCoupon requestUserCoupon) {

        ResponseUserCoupon returnUserCouponDto = couponService.getCoupon(requestUserCoupon);

        return ResponseEntity.ok(returnUserCouponDto);
    }

}
