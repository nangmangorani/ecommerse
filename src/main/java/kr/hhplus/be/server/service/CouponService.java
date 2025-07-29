package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.dto.coupon.ResponseUserCoupon;
import kr.hhplus.be.server.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponHistService couponHistService;
    private final PointService pointService;

    public CouponService(CouponRepository couponRepository, CouponHistService couponHistService, PointService pointService) {
        this.couponRepository = couponRepository;
        this.couponHistService = couponHistService;
        this.pointService = pointService;
    }

    public ResponseUserCoupon getCoupon(RequestUserCoupon requestUserCoupon) {

        // 1. 발급하려는 쿠폰을 hist에서 사용자가 받았는지 조회
        Boolean couponYn = couponHistService.getCouponHist(requestUserCoupon);

        if(couponYn) {
            throw new RuntimeException("쿠폰 이미 ㅇㅇ");
        }

        // 쿠폰발급
        Coupon coupon = couponRepository.findById(requestUserCoupon.couponId())
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        coupon.issueCoupon();

        // 쿠폰이력추가
        CouponHist couponHist = couponHistService.addCouponHist(requestUserCoupon, coupon);

        ResponseUserCoupon responseUserCoupon = new ResponseUserCoupon(
                couponHist.getCouponId(),
                couponHist.getUserId(),
                couponHist.getProductId()
        );

        return responseUserCoupon;
    }

}
