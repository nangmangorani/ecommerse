package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
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
    public User getUserInfo(long userId, UserStatus status) {
        return userRepository.findByIdAndStatus(userId, status)
                .orElseThrow(() -> new CustomException("사용자가 존재하지 않습니다."));
    }

    public User getUserAndCheckBalance(RequestOrder requestOrder) {

        User user = userRepository.findByIdAndStatus(requestOrder.userId(), UserStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("사용자가 존재하지 않습니다."));

        user.checkPoint(requestOrder.requestPrice(), user.getPoint());

        return user;
    }

    @Transactional
    public void deductPointsWithLock(Long userId, long amount) {

        User user = userRepository.findByIdAndStatusWithLock(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다"));

        user.usePoint(amount);
    }

    @Transactional
    public void refundPoints(Long userId, long amount) {
        User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다: " + userId));

        user.addPoint((int) amount);
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public ResponseUserPoint chargePoint(RequestPointCharge requestPointCharge) {

        User user = getUserInfo(requestPointCharge.userId(), UserStatus.ACTIVE);

        User returnUser = paymentService.chargePoint(user, requestPointCharge);

        return ResponseUserPoint.from(returnUser);
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        User user = getUserInfo(id, UserStatus.ACTIVE);

        if (user.getPoint() < 0) {
            throw new RuntimeException("포인트는 음수가 불가능");
        }

        return ResponseUserPoint.from(user);
    }


}
