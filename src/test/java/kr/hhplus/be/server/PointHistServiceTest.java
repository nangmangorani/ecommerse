package kr.hhplus.be.server;

import kr.hhplus.be.server.enums.TransactionType;
import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.repository.PointHistRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.service.PointHistService;
import kr.hhplus.be.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;



public class PointHistServiceTest {

    @InjectMocks
    private PointHistService pointHistService;

    @InjectMocks
    private UserService userService;


    @Mock
    private UserRepository userRepository;

    @Mock
    private PointHistRepository pointHistRepository;

    long id = 1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 포인트 조회 관련 테스트
     * 1. 회원이 존재하지 않는 경우
     * 2. 회원이 존재하지만 비활성화 상태인 경우
     * 3. id값이 음수로 넘어올 경우
     * 4. 포인트의 경계값 (-1, 0 ,1) 체크
     */

    /**
     * 포인트 충전 테스트
     * 1. 충전하려는 포인트가 양수가 아닐 경우
     * 2. 포인트 충전 후 이력에 올바르게 쌓이지 않은 경우
     */
    @Test
    @DisplayName("충전하려는 포인트가 양수가 아닐 경우")
    void 충전하려는_포인트가_양수가_아닐_경우() {

        Optional<User> user = Optional.of(new User(1, "이승준", UserStatus.ACTIVE, 1000L));
        RequestPointCharge requestPointCharge = new RequestPointCharge(1, -1L);

        given(userRepository.findById(id)).willReturn(user);

        assertThatThrownBy(() -> userService.chargePoint(requestPointCharge))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @DisplayName("포인트 충전 후 이력에 올바르게 쌓이지 않은 경우")
    void 포인트_충전후_이력에_올바르게_쌓이지_않는경우() {

        RequestPointCharge requestPointCharge = new RequestPointCharge(1, 100L);
        PointHist pointHist = new PointHist(TransactionType.CHARGE, 100L, 1000L,1);

        given(pointHistRepository.save(pointHist)).willReturn(null);

        assertThatThrownBy(() -> userService.chargePoint(requestPointCharge))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포인트 충전 후 이력이 올바르게 쌓인 경우")
    void 포인트_충전후_이력이_올바르게_쌓인_경우() {

        Optional<User> user = Optional.of(new User(1, "이승준", UserStatus.ACTIVE, 1000L));
        RequestPointCharge requestPointCharge = new RequestPointCharge(1, 100L);
        PointHist pointHist = new PointHist(TransactionType.CHARGE, 100L, 1000L,1);

        given(userRepository.findById(requestPointCharge.userId())).willReturn(user);
        given(pointHistRepository.save(pointHist)).willReturn(pointHist);

        ResponseUserPoint result = userService.chargePoint(requestPointCharge);

        assertThat(result.userId()).isEqualTo(1);
        assertThat(result.userName()).isEqualTo("이승준");
        assertThat(result.userPoint()).isEqualTo(1100L);

    }

}
