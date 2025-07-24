package kr.hhplus.be.repository;

import kr.hhplus.be.domain.PointHist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<PointHist, Long> {

    PointHist save(PointHist pointHist);

}
