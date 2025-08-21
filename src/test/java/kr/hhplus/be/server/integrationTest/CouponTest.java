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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@Slf4j
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
    private RedisTemplate<String, Object> redisTemplate;

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
        log.info("테스트 환경 초기화 시작");

        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.debug("Redis 캐시 완전 초기화 완료");

        couponRepository.deleteAll();
        couponHistRepository.deleteAll();
        log.debug("DB 데이터 초기화 완료");

        List<Coupon> testCoupons = Arrays.asList(
                new Coupon("테스트쿠폰1", CouponStatus.ACTIVE, 20, 100, 50, 1L),
                new Coupon("테스트쿠폰2", CouponStatus.ACTIVE, 15, 100, 50, 2L),
                new Coupon("테스트쿠폰3", CouponStatus.ACTIVE, 25, 100, 50, 3L),
                new Coupon("테스트쿠폰4", CouponStatus.ACTIVE, 30, 100, 50, 4L),
                new Coupon("테스트쿠폰5", CouponStatus.ACTIVE, 10, 100, 50, 5L),
                new Coupon("테스트쿠폰6", CouponStatus.ACTIVE, 35, 100, 50, 6L)
        );

        couponRepository.saveAll(testCoupons);
        log.debug("테스트 쿠폰 {} 개 생성 완료", testCoupons.size());

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
        log.debug("쿠폰 이력 {} 개 생성 완료", couponHists.size());

        for (CouponHist hist : couponHists) {
            String bitmapKey = "coupon:bitmap:" + hist.getCouponId();
            redisTemplate.opsForValue().setBit(bitmapKey, hist.getUserId(), true);

            String timestampKey = "coupon:timestamp:" + hist.getCouponId() + ":" + hist.getUserId();
            redisTemplate.opsForValue().set(timestampKey, System.currentTimeMillis(), Duration.ofDays(7));
        }

        Map<Long, Long> couponCounts = couponHists.stream()
                .collect(Collectors.groupingBy(CouponHist::getCouponId, Collectors.counting()));

        for (Map.Entry<Long, Long> entry : couponCounts.entrySet()) {
            String countKey = "coupon:count:" + entry.getKey();
            redisTemplate.opsForValue().set(countKey, entry.getValue(), Duration.ofDays(7));
        }

        log.debug("Redis 비트맵 및 카운트 설정 완료 - 쿠폰별 카운트: {}", couponCounts);
        log.info("테스트 환경 초기화 완료");
    }

    @Test
    @DisplayName("쿠폰 발급이력이 있는 경우")
    void hasCouponIssuanceHistory() throws Exception {
        log.info("=== 쿠폰 중복 발급 방지 테스트 시작 ===");

        RequestUserCoupon request = new RequestUserCoupon(1L,1L,1L);
        String requestBodyJson = objectMapper.writeValueAsString(request);

        log.debug("테스트 요청: userId={}, couponId={}", request.userId(), request.couponId());

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰을 이미 발급받았음"));

        log.info("중복 발급 방지 검증 완료 - 기대한 대로 발급 실패");
    }

    @Test
    @DisplayName("발급하려는 쿠폰이 존재하지 않을 때")
    void noCoupon() throws Exception {
        log.info("=== 존재하지 않는 쿠폰 발급 시도 테스트 시작 ===");

        RequestUserCoupon request = new RequestUserCoupon(1L,123L,1L);
        String requestBodyJson = objectMapper.writeValueAsString(request);

        log.debug("존재하지 않는 쿠폰 ID로 테스트: couponId={}", request.couponId());

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰을 찾을 수 없음"));

        log.info("존재하지 않는 쿠폰 처리 검증 완료");
    }

    @Test
    @DisplayName("쿠폰 수량 부족으로 발급 실패")
    void issueCouponIfAvailable() throws Exception {
        log.info("=== 쿠폰 수량 부족 테스트 시작 ===");

        coupon = new Coupon(
                "쿠폰1",
                CouponStatus.ACTIVE,
                20,
                10,
                0,
                1L
        );

        Coupon returnCoupon = couponRepository.save(coupon);
        log.debug("수량 소진 쿠폰 생성: couponId={}, remainQuantity={}", returnCoupon.getId(), returnCoupon.getRemainQuantity());

        RequestUserCoupon request = new RequestUserCoupon(3L,returnCoupon.getId(),1L);
        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("쿠폰 소진"));

        log.info("쿠폰 수량 소진 처리 검증 완료");
    }

    @Test
    @DisplayName("쿠폰 정상 발급")
    void successCoupon() throws Exception {
        log.info("=== 쿠폰 정상 발급 테스트 시작 ===");

        coupon = new Coupon(
                "쿠폰1",
                CouponStatus.ACTIVE,
                20,
                10,
                3,
                1L
        );

        Coupon returnCoupon = couponRepository.save(coupon);
        log.debug("발급 가능 쿠폰 생성: couponId={}, remainQuantity={}", returnCoupon.getId(), returnCoupon.getRemainQuantity());

        RequestUserCoupon request = new RequestUserCoupon(7L, returnCoupon.getId(), 1L);
        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(request.userId()))
                .andExpect(jsonPath("$.couponId").value(returnCoupon.getId()))
                .andExpect(jsonPath("$.productId").value(returnCoupon.getProductId()));

        log.info("쿠폰 정상 발급 검증 완료: userId={}, couponId={}", request.userId(), returnCoupon.getId());
    }

    @Test
    @DisplayName("쿠폰 동시성 테스트 : 1개의 쿠폰에 여러명 동시 접근")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentCouponIssue() throws InterruptedException {
        log.info("=== Redis 기반 쿠폰 동시성 테스트 시작 ===");

        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.debug("동시성 테스트를 위한 Redis 초기화 완료");

        coupon = new Coupon(
                "동시성테스트쿠폰",
                CouponStatus.ACTIVE,
                20,
                100,
                100,
                1L
        );
        Coupon returnCoupon = couponRepository.save(coupon);
        couponRepository.flush();
        log.debug("동시성 테스트 쿠폰 생성: couponId={}, maxQuantity={}, remainQuantity={}",
                returnCoupon.getId(), returnCoupon.getMaxQuantity(), returnCoupon.getRemainQuantity());

        final Long couponIdToIssue = returnCoupon.getId();
        final int REDIS_MAX_COUNT = 1;

        String maxCountKey = "coupon:max:" + couponIdToIssue;
        redisTemplate.opsForValue().set(maxCountKey, REDIS_MAX_COUNT, Duration.ofDays(1));
        log.debug("Redis 최대 발급 수량 설정: couponId={}, maxCount={}", couponIdToIssue, REDIS_MAX_COUNT);

        final int numberOfThreads = 100;
        final Long baseUserId = 1000L;

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        ExecutorService executorService = Executors.newFixedThreadPool(32);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        log.info("동시성 테스트 시작: {}명의 사용자가 동시에 쿠폰 발급 요청", numberOfThreads);

        IntStream.range(0, numberOfThreads).forEach(i -> executorService.submit(() -> {
            try {
                startLatch.await();

                Long currentUserId = baseUserId + i;
                RequestUserCoupon request = new RequestUserCoupon(currentUserId, couponIdToIssue, 1L);

                String requestBodyJson = objectMapper.writeValueAsString(request);

                MvcResult result = mockMvc.perform(post("/coupons/issue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                        .andReturn();

                int status = result.getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                    log.debug("성공한 사용자: {}", currentUserId);
                } else {
                    failCount.incrementAndGet();
                    String response = result.getResponse().getContentAsString();
                    log.debug("실패한 사용자: {}, 응답: {}", currentUserId, response);
                }

            } catch (Exception e) {
                failCount.incrementAndGet();
                log.error("요청 처리 중 예외 발생: userId={}, error={}", baseUserId + i, e.getMessage());
            } finally {
                endLatch.countDown();
            }
        }));

        startLatch.countDown();
        endLatch.await();

        executorService.shutdown();

        log.info("=== 동시성 테스트 결과 ===");
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(numberOfThreads - 1);

        String countKey = "coupon:count:" + couponIdToIssue;
        Object redisCount = redisTemplate.opsForValue().get(countKey);
        assertThat(redisCount).isNotNull();
        assertThat(((Number) redisCount).longValue()).isEqualTo(1L);
        log.info("Redis 카운트: {}", redisCount);

        String bitmapKey = "coupon:bitmap:" + couponIdToIssue;
        int issuedUserCount = 0;
        Long issuedUserId = null;

        for (long userId = baseUserId; userId < baseUserId + numberOfThreads; userId++) {
            Boolean isIssued = redisTemplate.opsForValue().getBit(bitmapKey, userId);
            if (Boolean.TRUE.equals(isIssued)) {
                issuedUserCount++;
                issuedUserId = userId;
            }
        }

        assertThat(issuedUserCount).isEqualTo(1);
        assertThat(issuedUserId).isNotNull();
        assertThat(issuedUserId).isBetween(baseUserId, baseUserId + numberOfThreads - 1);
        log.info("발급받은 사용자: {}", issuedUserId);

        String timestampKey = "coupon:timestamp:" + couponIdToIssue + ":" + issuedUserId;
        Object timestamp = redisTemplate.opsForValue().get(timestampKey);
        assertThat(timestamp).isNotNull();
        log.info("발급 시간: {}", timestamp);

        Coupon updatedCoupon = couponRepository.findById(couponIdToIssue).orElseThrow();
        assertThat(updatedCoupon.getRemainQuantity()).isEqualTo(100);
        log.info("DB remainQuantity: {} (변경되지 않음)", updatedCoupon.getRemainQuantity());

        log.info("=== Redis 기반 동시성 제어 검증 완료 ===");
        log.info("{}명 중 1명만 성공적으로 발급받아 동시성 제어가 정상 작동함", numberOfThreads);
    }
}