package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(String status);
    List<Product> findTop5ByOrderBySellQuantityDesc();
}
