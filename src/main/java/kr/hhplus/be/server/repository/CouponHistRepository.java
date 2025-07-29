package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.CouponHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponHistRepository extends JpaRepository<CouponHist, Long> {

    Optional<CouponHist> findByCouponIdAndUserId(long couponId, long userId);

}
