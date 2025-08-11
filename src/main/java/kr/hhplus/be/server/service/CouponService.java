package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.dto.coupon.ResponseUserCoupon;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.CouponRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponHistService couponHistService;

    public CouponService(CouponRepository couponRepository, CouponHistService couponHistService) {
        this.couponRepository = couponRepository;
        this.couponHistService = couponHistService;
    }

    @Transactional
    public ResponseUserCoupon getCoupon(RequestUserCoupon requestUserCoupon) {

        // 1. 발급하려는 쿠폰을 hist에서 사용자가 받았는지 조회
        Boolean couponYn = couponHistService.getCouponHist(requestUserCoupon);

        if(couponYn) {
            throw new CustomException("쿠폰을 이미 발급받았음");
        }


        // 쿠폰발급
        Coupon coupon = couponRepository.findById(requestUserCoupon.couponId())
                .orElseThrow(() -> new CustomException("쿠폰을 찾을 수 없음"));

        try {
            // 쿠폰수량 감소
            coupon.issueCoupon();

            couponRepository.save(coupon);
        } catch (CustomException e) {
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new CustomException("쿠폰 발급에 실패했습니다. (동시성 충돌 발생)");
        } catch (Exception e) {
            throw new CustomException("알 수 없는 오류 발생");
        }

        // 쿠폰이력추가
        CouponHist couponHist = couponHistService.addCouponHist(requestUserCoupon, coupon);

        ResponseUserCoupon responseUserCoupon = new ResponseUserCoupon(
                couponHist.getUserId(),
                couponHist.getCouponId(),
                couponHist.getProductId()
        );

        return responseUserCoupon;
    }

    public Coupon searchCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException("쿠폰없음"));
    }

    public Coupon searchCouponByProductId(Long productId) {
        return couponRepository.findCouponByProductIdAndStatus(productId, CouponStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("상품에 부합한 쿠폰이 없음"));
    }

    public long calculateDiscountedPrice(Product product, Coupon coupon) {

        long expectedDiscountPrice;

        if (coupon != null) {
            expectedDiscountPrice = product.getPrice() - (product.getPrice() * coupon.getDiscountPercent() / 100);
        } else {
            expectedDiscountPrice = product.getPrice();
        }

        return expectedDiscountPrice;
    }



}
