package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.repository.PointRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * 포인트 충전
     */
    @Transactional
    public ResponseUserPoint chargePoint(RequestPointCharge requestPointCharge) {

        if(requestPointCharge.userPoint() < 0) {
            throw new RuntimeException();
        }

        // 사용자 조회
        User user = pointRepository.getPointById(requestPointCharge.userId());

        if(user == null) {
            throw new RuntimeException();
        }

        if(user.getPoint() < 0) {
            throw new RuntimeException();
        }

        // 포인트 충전
        user.addPoint(requestPointCharge.userPoint());

        // 포인트 이력 업데이트
        PointHist pointHist = new PointHist(
                user.getId(),
                TransactionType.CHARGE,
                requestPointCharge.userPoint(),
                user.getPoint(),
                LocalDateTime.now()
        );

        PointHist returnHist = pointRepository.save(pointHist);

        return ResponseUserPoint.from(user);
    }

}
