package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c JOIN Product p ON c.productId = p.id " +
            "WHERE c.status = :couponStatus AND p.id = :productId")
    Optional<Coupon> findCouponByProductIdAndStatus(@Param("productId") Long productId,
                                                    @Param("couponStatus") String couponStatus);

}
