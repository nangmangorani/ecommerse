package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.enums.CouponHistStatus;
import kr.hhplus.be.server.repository.CouponHistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponHistService {

    private final CouponHistRepository couponHistRepository;

    public boolean getCouponHist(RequestUserCoupon requestUserCoupon) {

        Optional<CouponHist> couponHist = couponHistRepository.findByCouponIdAndUserId(
                requestUserCoupon.couponId(),
                requestUserCoupon.userId()
        );

        return couponHist.isPresent();
    }

    public CouponHist addCouponHist(RequestUserCoupon requestUserCoupon, Coupon coupon) {

        CouponHist couponHist = new CouponHist(
                requestUserCoupon.couponId(),
                requestUserCoupon.userId(),
                requestUserCoupon.productId(),
                CouponHistStatus.ISSUED
        );

        return couponHistRepository.save(couponHist);
    }

}
