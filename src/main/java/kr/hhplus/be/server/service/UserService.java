package kr.hhplus.be.server.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 사용자조회
    public User getUserInfo(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));
    }

    public User getUserAndCheckBalance(long userId, long requiredPrice) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));

        if (user.getPoint() < requiredPrice) {
            throw new RuntimeException("잔고 부족");
        }

        return user;
    }

}
