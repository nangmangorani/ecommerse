package kr.hhplus.be.server.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.CouponHist;
import kr.hhplus.be.server.dto.coupon.RequestUserCoupon;
import kr.hhplus.be.server.enums.CouponHistStatus;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.repository.CouponHistRepository;
import kr.hhplus.be.server.repository.CouponRepository;
import kr.hhplus.be.server.service.CouponHistService;
import kr.hhplus.be.server.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class CouponTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponHistService couponHistService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponHistRepository couponHistRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private List<CouponHist> couponHists;
    private Coupon coupon;

    /**
     * 쿠폰 발급은 발급이력을 확인하고 발급이력이 없을경우 추가해야하므로
     * 이력을 먼저 쌓아야 테스트 가능
     */
    @BeforeEach
    void setUp() {
        couponRepository.deleteAll();
        couponHistRepository.deleteAll();

        couponHists = Arrays.asList(
                new CouponHist(1L, 1L, 1L, CouponHistStatus.ISSUED),
                new CouponHist(1L, 2L, 1L, CouponHistStatus.ISSUED),
                new CouponHist(2L, 1L, 2L, CouponHistStatus.ISSUED),
                new CouponHist(3L, 3L, 3L, CouponHistStatus.ISSUED),
                new CouponHist(4L, 4L, 4L, CouponHistStatus.ISSUED),
                new CouponHist(5L, 4L, 5L, CouponHistStatus.ISSUED),
                new CouponHist(5L, 6L, 5L, CouponHistStatus.ISSUED),
                new CouponHist(6L, 5L, 6L, CouponHistStatus.ISSUED),
                new CouponHist(6L, 4L, 6L, CouponHistStatus.ISSUED),
                new CouponHist(6L, 2L, 6L, CouponHistStatus.ISSUED)
        );

        couponHistRepository.saveAll(couponHists);
    }

    @Test
    @DisplayName("쿠폰 발급이력이 있는 경우")
    void hasCouponIssuanceHistory() throws Exception {

        RequestUserCoupon request = new RequestUserCoupon(1L,1L,1L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰을 이미 발급받았음"));
    }

    @Test
    @DisplayName("발급하려는 쿠폰이 존재하지 않을 때")
    void noCoupon() throws Exception {

        RequestUserCoupon request = new RequestUserCoupon(1L,123L,1L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰을 찾을 수 없음"));
    }

    @Test
    @DisplayName("쿠폰 수량 부족으로 발급 실패")
    void issueCouponIfAvailable() throws Exception {

        coupon = new Coupon(
                "쿠폰1",
                CouponStatus.ACTIVE,
                20,
                10,
                0,
                1L
        );

        Coupon returnCoupon = couponRepository.save(coupon);

        RequestUserCoupon request = new RequestUserCoupon(3L,returnCoupon.getId(),1L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰 소진"));

    }

    @Test
    @DisplayName("쿠폰 정상 발급")
    void successCoupon() throws Exception {

        coupon = new Coupon(
                "쿠폰1",
                CouponStatus.ACTIVE,
                20,
                10,
                3,
                1L
        );

        Coupon returnCoupon = couponRepository.save(coupon);

        RequestUserCoupon request = new RequestUserCoupon(7L, returnCoupon.getId(), 1L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(request.userId()))
                .andExpect(jsonPath("$.couponId").value(returnCoupon.getId()))
                .andExpect(jsonPath("$.productId").value(returnCoupon.getProductId()));
    }

    @Test
    @DisplayName("쿠폰 동시성 테스트 : 1개의 쿠폰에 여러명 동시 접근")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCouponIssue() throws InterruptedException {
        coupon = new Coupon(
                "쿠폰1",
                CouponStatus.ACTIVE,
                20,
                10,
                1,
                1L
        );
        Coupon returnCoupon = couponRepository.save(coupon);
        couponRepository.flush();

        final int numberOfThreads = 100; // 동시 요청할 사용자 수
        final Long couponIdToIssue = returnCoupon.getId();
        final Long baseUserId = 1L; // 사용자 ID 시작 번호

        // CountDownLatch: 모든 스레드가 동시에 시작하도록 조율 (startLatch),
        // 모든 스레드가 완료될 때까지 메인 스레드 대기 (endLatch)
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        // ExecutorService: 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32); // 적절한 스레드 풀 크기

        // 성공 및 실패 카운트 (AtomicInteger는 스레드 안전한 카운터)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when (여러 스레드에서 동시에 쿠폰 발급 요청)
        IntStream.range(0, numberOfThreads).forEach(i -> executorService.submit(() -> {
            try {
                startLatch.await(); // 모든 스레드가 동시에 시작할 때까지 대기

                Long currentUserId = baseUserId + i;
                RequestUserCoupon request = new RequestUserCoupon(currentUserId, couponIdToIssue, 1L);

                String requestBodyJson = objectMapper.writeValueAsString(request);

                MvcResult result = mockMvc.perform(post("/coupons/issue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                        .andReturn(); // 응답 결과 가져오기

                // 응답 상태 코드에 따라 성공/실패 카운트 증가
                int status = result.getResponse().getStatus();
                if (status == 200) { // HTTP 200 OK
                    successCount.incrementAndGet();
                } else { // 그 외 (예: 400 Bad Request, 409 Conflict, 500 Internal Server Error)
                    failCount.incrementAndGet();
                }

            } catch (Exception e) {
                failCount.incrementAndGet();
                System.err.println("요청 처리 중 예외 발생: " + e.getMessage());
            } finally {
                endLatch.countDown(); // 작업 완료를 알림
            }
        }));

        startLatch.countDown(); // 모든 스레드에 시작 신호
        endLatch.await(); // 모든 스레드가 완료될 때까지 대기

        executorService.shutdown(); // 스레드 풀 종료

        // 1. 성공 요청은 정확히 1건
        assertThat(successCount.get()).isEqualTo(1);

        // 1. 실제로 발급된 쿠폰 이력은 1개여야 함
        long issuedCouponCount = couponHistRepository.count(); // 모든 이력 카운트
        assertThat(issuedCouponCount).isEqualTo(couponHists.size() + 1); // 기존 10개 + 새로 발급된 1개

        // 2. 성공한 요청은 정확히 1개여야 함
        assertThat(successCount.get()).isEqualTo(1);

        // 3. 실패한 요청은 (총 요청 수 - 성공한 요청 수)여야 함
        assertThat(failCount.get()).isEqualTo(numberOfThreads - 1);

        // 4. 쿠폰의 잔여 수량이 0이 되었는지 확인
        Coupon updatedCoupon = couponRepository.findById(couponIdToIssue).orElseThrow();
        assertThat(updatedCoupon.getRemainQuantity()).isEqualTo(0);
        assertThat(updatedCoupon.getVersion()).isGreaterThan(coupon.getVersion()); // 버전 증가 확인 (낙관적 락)

    }

}
