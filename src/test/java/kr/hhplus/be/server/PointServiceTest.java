package kr.hhplus.be.server;

import kr.hhplus.be.server.domain.PointHist;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.dto.point.ResponseUserPoint;
import kr.hhplus.be.server.repository.PointRepository;
import kr.hhplus.be.server.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;



public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    long id = 1;

    @BeforeEach
    void setUp() {
        // @Mock, @InjectMocks 어노테이션 필드 초기화
        MockitoAnnotations.openMocks(this);

    }

    /**
     * 포인트 조회 관련 테스트
     * 1. 회원이 존재하지 않는 경우
     * 2. 회원이 존재하지만 비활성화 상태인 경우
     * 3. id값이 음수로 넘어올 경우
     * 4. 포인트의 경계값 (-1, 0 ,1) 체크
     */
    @Test
    @DisplayName("회원이 존재하지 않는 경우")
    void 회원이_존재하지_않는_경우() {

        // given
        given(pointRepository.getPointById(id)).willReturn(null);

        // when, then
        assertThatThrownBy(() -> pointService.getPoint(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포인트가 음수일 경우")
    void 포인트가_음수일_경우() {

        User user = new User(1, "이승준", "Y", -1L);

        // given
        given(pointRepository.getPointById(id)).willReturn(user);

        // when, then
        assertThatThrownBy(() -> pointService.getPoint(id))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("정상적으로 조회")
    void 정상적으로_조회() {

        User user = new User(1,"이승준", "Y", 1000L);

        // given
        given(pointRepository.getPointById(id)).willReturn(user);

        // when
        ResponseUserPoint result = pointService.getPoint(id);

        // then
        assertThat(result.userId()).isEqualTo(1);
        assertThat(result.userName()).isEqualTo("이승준");
        assertThat(result.userPoint()).isEqualTo(1000L);

    }

    /**
     * 포인트 충전 테스트
     * 1. 충전하려는 포인트가 양수가 아닐 경우
     * 2. 포인트 충전 후 이력에 올바르게 쌓이지 않은 경우
     */
    @Test
    @DisplayName("충전하려는 포인트가 양수가 아닐 경우")
    void 충전하려는_포인트가_양수가_아닐_경우() {

        User user = new User(1,"이승준", "Y", 1000L);
        RequestPointCharge requestPointCharge = new RequestPointCharge(1, -1L);

        // given
        given(pointRepository.getPointById(id)).willReturn(user);

        // when, then
        assertThatThrownBy(() -> pointService.chargePoint(requestPointCharge))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @DisplayName("포인트 충전 후 이력에 올바르게 쌓이지 않은 경우")
    void 포인트_충전후_이력에_올바르게_쌓이지_않는경우() {

        RequestPointCharge requestPointCharge = new RequestPointCharge(1, 100L);
        PointHist pointHist = new PointHist(1, TransactionType.CHARGE, 100L, 1000L, LocalDateTime.now());

        given(pointRepository.save(pointHist)).willReturn(null);

        // when, then
        assertThatThrownBy(() -> pointService.chargePoint(requestPointCharge))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("포인트 충전 후 이력이 올바르게 쌓인 경우")
    void 포인트_충전후_이력이_올바르게_쌓인_경우() {

        User user = new User(1,"이승준", "Y", 1000L);
        RequestPointCharge requestPointCharge = new RequestPointCharge(1, 100L);
        PointHist pointHist = new PointHist(1, TransactionType.CHARGE, 100L, 1000L, LocalDateTime.now());

        given(pointRepository.getPointById(requestPointCharge.userId())).willReturn(user);
        given(pointRepository.save(pointHist)).willReturn(pointHist);

        // when
        ResponseUserPoint result = pointService.chargePoint(requestPointCharge);

        // then
        assertThat(result.userId()).isEqualTo(1);
        assertThat(result.userName()).isEqualTo("이승준");
        assertThat(result.userPoint()).isEqualTo(1100L);

    }

}
