package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointRepository extends JpaRepository<User, Long> {

    User getPointById(long id);

    PointHist save(PointHist pointHist);

}
