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

    private Product product;

    private User user;

    private Order order;

    private Payment payment;

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

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1200000L,960000L, true);

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

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 2, 2400000L,1920000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청수량보다 재고 부족"));

    }

    @Test
    @DisplayName("결제 성공")
    void successPayment() throws Exception {
        user = new User("테스트유저1", UserStatus.ACTIVE, 5000000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1200000L,960000L, true);

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
        // 충분한 재고와 포인트 설정
        product = new Product("대용량상품", ProductStatus.ACTIVE, 1000, 1000, 100000L, "테스트");
        product = productRepository.save(product);

        user = new User("테스트유저1", UserStatus.ACTIVE, 5000000L);
        user = userRepository.save(user);

        int threadCount = 100; // 100개 스레드
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

        int threadCount = 50; // 상품 50개 요청
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

        // 정확히 10개만 성공해야 함
        assertThat(successCount.get()).isBetween(9, 10);
        assertThat(failCount.get()).isBetween(40,41);
    }




}
