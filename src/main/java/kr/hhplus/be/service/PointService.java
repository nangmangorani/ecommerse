package kr.hhplus.be.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.TransactionType;
import kr.hhplus.be.domain.PointHist;
import kr.hhplus.be.domain.User;
import kr.hhplus.be.dto.point.RequestPointCharge;
import kr.hhplus.be.dto.point.ResponseUserPoint;
import kr.hhplus.be.repository.PointRepository;
import kr.hhplus.be.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    public PointService(PointRepository pointRepository, UserRepository userRepository) {
        this.pointRepository = pointRepository;
        this.userRepository = userRepository;
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        Optional<User> optionalUser = userRepository.getPointById(id);

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
        User user = userRepository.getPointById(requestPointCharge.userId())
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
