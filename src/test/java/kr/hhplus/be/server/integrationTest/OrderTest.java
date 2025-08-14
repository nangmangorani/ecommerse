package kr.hhplus.be.server.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.enums.CouponStatus;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.repository.*;
import kr.hhplus.be.server.service.OrderService;
import kr.hhplus.be.server.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class OrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PointHistRepository pointHistRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    private Product product;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        pointHistRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
        couponRepository.deleteAll();

        product = new Product("iPhone 15", ProductStatus.ACTIVE, 1, 25, 1200000L, "전자제품");
        product = productRepository.save(product);

        coupon = new Coupon("쿠폰1", CouponStatus.ACTIVE, 20, 10, 3, product.getId());
        coupon = couponRepository.save(coupon);
    }

    @Test
    @DisplayName("사용자 잔고 부족")
    void hasSufficientBalance() throws Exception {

        user = new User("테스트유저1", UserStatus.ACTIVE, 5000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1200000L, 960000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("잔고 부족"));
    }

    @Test
    @DisplayName("요청수량보다 재고 부족")
    void hasSufficientQuantity() throws Exception {
        user = new User("테스트유저1", UserStatus.ACTIVE, 5000000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 2, 2400000L, 1920000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청수량보다 상품 재고가 부족합니다."));

    }

    @Test
    @DisplayName("결제 성공")
    void successPayment() throws Exception {
        user = new User("테스트유저1", UserStatus.ACTIVE, 5000000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1200000L, 960000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value(returnUser.getName()))
                .andExpect(jsonPath("$.couponYn").value(request.couponYn()))
                .andExpect(jsonPath("$.originalPrice").value(request.originalPrice()))
                .andExpect(jsonPath("$.discountPrice").value(request.requestPrice()));

    }

    @Test
    @DisplayName("대용량 동시 주문 처리")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void massiveConcurrentOrders() throws InterruptedException {
        product = new Product("대용량상품", ProductStatus.ACTIVE, 1000, 1000, 100000L, "테스트");
        product = productRepository.save(product);

        user = new User("테스트유저1", UserStatus.ACTIVE, 20000000L);
        user = userRepository.save(user);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    RequestOrder request = new RequestOrder(
                            user.getId(),
                            product.getId(),
                            null,
                            1,
                            100000L,
                            100000L,
                            false
                    );

                    String requestBodyJson = objectMapper.writeValueAsString(request);

                    ResultActions result = mockMvc.perform(post("/order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBodyJson));

                    if (result.andReturn().getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        assertThat(endTime - startTime).isLessThan(30000);
    }

    @Test
    @DisplayName("동시성 - 재고 부족으로 일부 주문 실패")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentOrdersWithInsufficientStock() throws InterruptedException {

        product = new Product("제한재고상품", ProductStatus.ACTIVE, 10, 10, 100000L, "테스트");
        product = productRepository.save(product);

        user = new User("테스트유저1", UserStatus.ACTIVE, 50000000L);
        user = userRepository.save(user);

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    RequestOrder request = new RequestOrder(
                            user.getId(), product.getId(), null, 1,
                            100000L, 100000L, false
                    );

                    String requestBodyJson = objectMapper.writeValueAsString(request);
                    ResultActions result = mockMvc.perform(post("/order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBodyJson));

                    if (result.andReturn().getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(successCount.get()).isBetween(9, 10);
        assertThat(failCount.get()).isBetween(40, 41);
    }

    @Test
    @DisplayName("Redis 연결 및 기본 동작 확인")
    void shouldConnectRedisAndWork() {
        String testKey = "test:redis:connection";
        RLock lock = redissonClient.getLock(testKey);

        assertDoesNotThrow(() -> {
            boolean acquired = lock.tryLock(1, 5, TimeUnit.SECONDS);
            assertThat(acquired).isTrue();
            lock.unlock();
        });
    }

    @Test
    @DisplayName("Redis 분산락 기반 대량 동시 주문 처리")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void massiveConcurrentOrdersWithRedisLock() throws InterruptedException {
        product = new Product("Redis락상품", ProductStatus.ACTIVE, 1000, 1000, 100000L, "테스트");
        product = productRepository.save(product);

        user = new User("테스트유저1", UserStatus.ACTIVE, 20000000L);
        user = userRepository.save(user);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                RLock lock = redissonClient.getLock("lock:product:" + product.getId());
                try {
                    lock.lock();

                    RequestOrder request = new RequestOrder(
                            user.getId(), product.getId(), null, 1, 100000L, 100000L, false
                    );

                    String requestBodyJson = objectMapper.writeValueAsString(request);
                    ResultActions result = mockMvc.perform(post("/order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBodyJson));

                    if (result.andReturn().getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Redis 분산락 TTL 만료 테스트 - TTL 지나면 다른 요청이 락 획득 가능")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldAcquireLockAfterTTLExpires() throws Exception {
        Product product = productRepository.save(
                new Product("TTL테스트상품", ProductStatus.ACTIVE, 5, 5, 100000L, "테스트")
        );
        User user = userRepository.save(
                new User("TTL테스트유저", UserStatus.ACTIVE, 50000000L)
        );

        RequestOrder request = new RequestOrder(
                user.getId(), product.getId(), null, 1, 100000L, 100000L, false
        );

        new Thread(() -> {
            try {
                String requestBodyJson = objectMapper.writeValueAsString(request);
                mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson));
                Thread.sleep(3000); // TTL보다 길게 점유
            } catch (Exception ignored) {}
        }).start();

        Thread.sleep(2500);

        String requestBodyJson2 = objectMapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(post("/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBodyJson2));

        int status = result.andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(finalProduct.getQuantity()).isEqualTo(3);
    }

}


