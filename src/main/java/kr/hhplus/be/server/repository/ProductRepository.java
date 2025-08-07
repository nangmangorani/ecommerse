package kr.hhplus.be.server.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(String status);
    List<Product> findTop5ByOrderBySellQuantityDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT p FROM Product p WHERE p.id = :productId AND p.status = :status")
    Optional<Product> findByIdAndStatusWithLock(@Param("productId") Long productId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdWithLock(@Param("productId") Long productId);
}
