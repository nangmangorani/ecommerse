package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository {

    Payment save(Payment payment);

}
