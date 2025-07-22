package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.repository.PointRepository;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private final PointRepository pointRepository;

    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        User user = pointRepository.getPointById(id);

        if(user == null) {
            throw new RuntimeException();
        }

        if(user.getPoint() < 0) {
            throw new RuntimeException();
        }

        return ResponseUserPoint.from(user);

    }

}
