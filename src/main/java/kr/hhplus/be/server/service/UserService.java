package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.order.RequestOrder;
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
public class UserService {

    private final UserRepository userRepository;
    private final PointHistService pointHistService;
    private final PaymentService paymentService;

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

}
