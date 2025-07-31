package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.PointHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<PointHist, Long> {
    
}
