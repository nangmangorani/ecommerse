package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.repository.CouponHistRepository;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
public class CouponHistService {

    private final CouponHistRepository couponHistRepository;

    public CouponHistService(CouponHistRepository couponHistRepository) {
        this.couponHistRepository = couponHistRepository;
    }

    // 사용자 쿠폰이력조회
    public boolean getCouponHist(RequestUserCoupon requestUserCoupon) {

        Optional<CouponHist> couponHist = couponHistRepository.findByCouponIdAndUserId(
                requestUserCoupon.couponId(),
                requestUserCoupon.userId()
        );

        return couponHist.isPresent();
    }

    // 쿠폰이력추가
    public CouponHist addCouponHist(RequestUserCoupon requestUserCoupon, Coupon coupon) {

        CouponHist couponHist = new CouponHist(
                requestUserCoupon.couponId(),
                requestUserCoupon.userId(),
                requestUserCoupon.productId(),
                "01"
        );

        return couponHistRepository.save(couponHist);
    }

}
