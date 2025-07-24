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
import java.util.Optional;

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

        Optional<User> optionalUser = pointRepository.getPointById(id);

        User user = optionalUser.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getPoint() < 0) {
            throw new RuntimeException();
        }

        return ResponseUserPoint.from(user);
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public ResponseUserPoint chargePoint(RequestPointCharge requestPointCharge) {

        if (requestPointCharge.userPoint() < 0) {
            throw new IllegalArgumentException();
        }

        // 사용자 조회 및 null 처리
        User user = pointRepository.getPointById(requestPointCharge.userId())
                .orElseThrow(() -> new RuntimeException());

        if (user.getPoint() < 0) {
            throw new IllegalStateException();
        }

        // 포인트 충전
        user.addPoint(requestPointCharge.userPoint());

        // 포인트 이력 저장
        PointHist pointHist = new PointHist(
                TransactionType.CHARGE,
                requestPointCharge.userPoint(),
                user.getPoint()
        );

        pointRepository.save(pointHist);

        return ResponseUserPoint.from(user);
    }

}
