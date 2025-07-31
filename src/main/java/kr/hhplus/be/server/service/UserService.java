package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

}
