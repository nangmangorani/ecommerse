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

    private final PaymentService paymentService;
    private final UserService userService;

    public PointService(UserRepository userRepository, PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    /**
     * 포인트 조회
     */
    public ResponseUserPoint getPoint(long id) {

        User user = userService.getUserInfo(id);

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

        User user = userService.getUserInfo(requestPointCharge.userId());

        user.addPoint(requestPointCharge.userPoint());

        User returnUser = paymentService.chargePoint(user, requestPointCharge);

        return ResponseUserPoint.from(returnUser);
    }

}
