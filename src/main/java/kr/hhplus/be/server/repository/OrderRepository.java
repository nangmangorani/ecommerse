package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository {
    Order save();
}
