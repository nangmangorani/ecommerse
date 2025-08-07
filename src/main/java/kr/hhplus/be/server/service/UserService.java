package kr.hhplus.be.server.service;

import kr.hhplus.be.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PointHistService pointHistService;

    private final PaymentService paymentService;
    public UserService(UserRepository userRepository, PointHistService pointHistService,  PaymentService paymentService) {
        this.userRepository = userRepository;
        this.pointHistService = pointHistService;
        this.paymentService = paymentService;
    }

    // 사용자조회
    public User getUserInfo(long userId, String status) {
        return userRepository.findByIdAndStatus(userId, status)
                .orElseThrow(() -> new CustomException("사용자가 존재하지 않습니다."));
    }

    public User getUserAndCheckBalance(long userId, long requiredPrice, String status) {

        User user = userRepository.findByIdAndStatus(userId, status)
                .orElseThrow(() -> new CustomException("사용자가 존재하지 않습니다."));

        if (user.getPoint() < requiredPrice) {
            throw new CustomException("잔고 부족");
        }

        return user;
    }

    @Transactional
    public void deductPointsWithLock(Long userId, long amount) {

        String status = "01";

        User user = userRepository.findByIdAndStatusWithLock(userId, status)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다"));

        user.usePoint(amount);
    }

    @Transactional
    public void refundPoints(Long userId, long amount) {
        User user = userRepository.findByIdAndStatus(userId, "01")
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다: " + userId));

        user.addPoint((int) amount);
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public ResponseUserPoint chargePoint(RequestPointCharge requestPointCharge) {

        String status = "01"; // 사용자 상태 정상

        User user = getUserInfo(requestPointCharge.userId(), status);

        User returnUser = paymentService.chargePoint(user, requestPointCharge);

        return ResponseUserPoint.from(returnUser);
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        String status = "01"; // 사용자 상태 정상

        User user = getUserInfo(id, status);

        if (user.getPoint() < 0) {
            throw new RuntimeException("포인트는 음수가 불가능");
        }

        return ResponseUserPoint.from(user);
    }


}
