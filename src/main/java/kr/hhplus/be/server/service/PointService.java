package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PointService {

    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public PointService(UserRepository userRepository, PaymentService paymentService) {
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        Optional<User> optionalUser = userRepository.findById(id);

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
            throw new IllegalArgumentException("음수!!");
        }

        // 사용자 조회 및 null 처리
        User user = userRepository.findById(requestPointCharge.userId())
                .orElseThrow(() -> new RuntimeException("조회없음!!"));

        if (user.getPoint() < 0) {
            throw new IllegalStateException();
        }

        User returnUser = paymentService.chargePoint(user, requestPointCharge);

        return ResponseUserPoint.from(returnUser);
    }

}
