package kr.hhplus.be.repository;

import kr.hhplus.be.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment save(Payment payment);

}
