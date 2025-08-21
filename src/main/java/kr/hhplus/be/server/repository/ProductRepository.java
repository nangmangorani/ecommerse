package kr.hhplus.be.server.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.enums.ProductStatus;
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

    List<Product> findByStatus(ProductStatus productStatus);

    List<Product> findTop5ByOrderBySellQuantityDesc();

    @Query(value = "SELECT p FROM Product p WHERE p.id = :productId AND p.status = :status")
    Optional<Product> findByIdAndStatus(@Param("productId") Long productId, ProductStatus status);

    List<Product> findByIdInAndStatus(List<Long> ids, ProductStatus status);
}