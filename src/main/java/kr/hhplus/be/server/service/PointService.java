package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PaymentService paymentService;

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        User user = getUserWithValidation(id);

        if (user.getPoint() < 0) {
            throw new RuntimeException("포인트는 음수가 불가능");
        }

        return ResponseUserPoint.from(user);
    }

    /**
     * 포인트 충전
     */
    @Transactional
    public ResponseUserPoint chargePoint(RequestPointCharge requestPointCharge) {

        User user = getUserWithValidation(requestPointCharge.userId());
        User returnUser = paymentService.chargePoint(user, requestPointCharge);

        return ResponseUserPoint.from(returnUser);
    }

    @Transactional
    public void deductPointsWithLock(Long userId, long amount) {

        User user = userRepository.findByIdAndStatusWithLock(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다"));

        user.usePoint(amount);
    }

    @Transactional
    public void refundPoints(Long userId, long amount) {
        User user = getUserWithValidation(userId);
        user.addPoint((int) amount);
    }

    private User getUserWithValidation(Long userId) {
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다: " + userId));
    }

}
