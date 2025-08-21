package kr.hhplus.be.server.integrationTest;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@Slf4j
public class ProductTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final int WARM_UP_REQUESTS = 100;
    private static final int TEST_REQUESTS = 1000;
    private static final int CONCURRENT_USERS = 50;

    private List<Product> products;

    @BeforeEach
    void setUp() {
        clearRedisData();
        productRepository.deleteAll();

        productService.invalidateTop5Cache();
        products = Arrays.asList(
                new Product("iPhone 15", ProductStatus.ACTIVE, 50, 25, 1200000L, "전자제품"),
                new Product("삼성 갤럭시 S24", ProductStatus.ACTIVE, 30, 15, 1100000L, "전자제품"),
                new Product("나이키 에어맥스", ProductStatus.ACTIVE, 100, 45, 150000L, "의류"),
                new Product("아디다스 운동화", ProductStatus.ACTIVE, 80, 32, 120000L, "의류"),
                new Product("맥북 프로 14인치", ProductStatus.ACTIVE, 20, 8, 2500000L, "전자제품"),
                new Product("스타벅스 아메리카노", ProductStatus.ACTIVE, 200, 180, 4500L, "음료"),
                new Product("무선 블루투스 이어폰", ProductStatus.ACTIVE, 150, 75, 80000L, "전자제품"),
                new Product("리바이스 청바지", ProductStatus.ACTIVE, 60, 28, 90000L, "의류"),
                new Product("프리미엄 커피원두 1kg", ProductStatus.ACTIVE, 40, 12, 35000L, "음료"),
                new Product("게이밍 키보드", ProductStatus.ACTIVE, 75, 41, 180000L, "전자제품")
        );

        productRepository.saveAll(products);

    }

    @Test
    @DisplayName("상품이 존재하지 않음")
    void noProductList() throws Exception {
        productRepository.deleteAll();

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("상품list 조회 성공")
    void successGetProductList() throws Exception {
        mockMvc.perform(get("/product/list"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(products.size()))
                .andExpect(jsonPath("$[0].productName").value(products.get(0).getName()))
                .andExpect(jsonPath("$[0].sellQuantity").value(products.get(0).getSellQuantity()))
                .andExpect(jsonPath("$[0].price").value(products.get(0).getPrice()));
    }

    @Test
    @DisplayName("상품list 5개 조회 성공")
    void successTop5List() throws Exception {
        mockMvc.perform(get("/product/list/top5"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("상품이 존재하지않아 상세조회 실패")
    void noProductTest() throws Exception {

        Long id = 999L;

        mockMvc.perform(get("/product/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("상품이 존재하지 않음"));

    }

    @Test
    @DisplayName("상품 상세조회 성공")
    void successProductTest() throws Exception {

        mockMvc.perform(get("/product/{id}", products.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(products.get(0).getId()))
                .andExpect(jsonPath("$.productName").value(products.get(0).getName()))
                .andExpect(jsonPath("$.sellQuantity").value(products.get(0).getSellQuantity()))
                .andExpect(jsonPath("$.productQuantity").value(products.get(0).getQuantity()))
                .andExpect(jsonPath("$.price").value(products.get(0).getPrice()))
                .andExpect(jsonPath("$.productType").value(products.get(0).getType()));
    }

    @Test
    @Order(1)
    @DisplayName("성능 비교: 캐시 적용 전 vs 후")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void comparePerformanceWithAndWithoutCache() throws InterruptedException {
        log.info("\n=== TOP5 상품 조회 성능 비교 테스트 ===\n");

        PerformanceResult withoutCache = measureDBPerformance();

        // 첫 번째 요청은 캐시 미스
        PerformanceResult withCache = measureCachePerformance();

        printComparisonResults(withoutCache, withCache);

        assertThat(withCache.averageResponseTime)
                .isLessThan(withoutCache.averageResponseTime)
                .as("캐시 적용 후 평균 응답시간이 개선되어야 합니다");

        assertThat(withCache.throughput)
                .isGreaterThan(withoutCache.throughput)
                .as("캐시 적용 후 처리량이 증가해야 합니다");
    }

    @Test
    @Order(2)
    @DisplayName("동시성 테스트: 캐시 히트율 및 성능")
    void testConcurrentPerformance() throws InterruptedException {
        log.info("\n=== 동시성 캐시 성능 테스트 ===\n");

        // 캐시 워밍업 (첫 요청으로 캐시 생성)
        productService.getProductListTop5();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(TEST_REQUESTS);

        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TEST_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    List<ResponseProduct> result = productService.getProductListTop5();
                    long requestTime = System.currentTimeMillis() - requestStart;

                    if (!result.isEmpty()) {
                        totalResponseTime.addAndGet(requestTime);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("요청 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        double averageResponseTime = (double) totalResponseTime.get() / successCount.get();
        double throughput = (double) successCount.get() / totalTime * 1000;

        log.info("동시성 테스트 결과:");
        log.info("- 총 요청 수: {}", TEST_REQUESTS);
        log.info("- 성공 요청 수: {}", successCount.get());
        log.info("- 동시 사용자 수: {}", CONCURRENT_USERS);
        log.info("- 총 실행 시간: {} ms", totalTime);
        log.info("- 평균 응답 시간: {:.2f} ms", averageResponseTime);
        log.info("- 처리량(TPS): {:.2f} requests/sec", throughput);
        log.info("- 캐시 적중률: 거의 100% (첫 요청 후 모든 요청이 캐시 히트)");
    }

    private PerformanceResult measureDBPerformance() throws InterruptedException {
        log.info(" 캐시 없이 DB 직접 조회 성능 측정 중...");

        productService.invalidateTop5Cache();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(TEST_REQUESTS);

        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < WARM_UP_REQUESTS; i++) {
            productService.invalidateTop5Cache();
            productService.getProductListTop5();
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TEST_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    productService.invalidateTop5Cache();
                    long requestStart = System.currentTimeMillis();
                    List<ResponseProduct> result = productService.getProductListTop5();
                    long requestTime = System.currentTimeMillis() - requestStart;

                    if (!result.isEmpty()) {
                        totalResponseTime.addAndGet(requestTime);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("DB 조회 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        double averageResponseTime = (double) totalResponseTime.get() / successCount.get();
        double throughput = (double) successCount.get() / totalTime * 1000;

        return new PerformanceResult(averageResponseTime, throughput, totalTime, successCount.get());
    }

    private PerformanceResult measureCachePerformance() throws InterruptedException {
        log.info(" 캐시 적용 성능 측정 중...");

        productService.getProductListTop5();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(TEST_REQUESTS);

        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TEST_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    List<ResponseProduct> result = productService.getProductListTop5();
                    long requestTime = System.currentTimeMillis() - requestStart;

                    if (!result.isEmpty()) {
                        totalResponseTime.addAndGet(requestTime);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("캐시 조회 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        double averageResponseTime = (double) totalResponseTime.get() / successCount.get();
        double throughput = (double) successCount.get() / totalTime * 1000;

        return new PerformanceResult(averageResponseTime, throughput, totalTime, successCount.get());
    }

    private void printComparisonResults(PerformanceResult withoutCache, PerformanceResult withCache) {
        log.info("");
        log.info("=".repeat(60));
        log.info("성능 비교 결과");
        log.info("=".repeat(60));

        log.info("");
        log.info("   캐시 적용 전 (DB 직접 조회):");
        log.info("   - 평균 응답시간: {:.2f} ms", withoutCache.averageResponseTime);
        log.info("   - 처리량(TPS): {:.2f} requests/sec", withoutCache.throughput);
        log.info("   - 총 실행시간: {} ms", withoutCache.totalTime);
        log.info("   - 성공 요청: {}개", withoutCache.successCount);

        log.info("");
        log.info(" 캐시 적용 후:");
        log.info("   - 평균 응답시간: {:.2f} ms", withCache.averageResponseTime);
        log.info("   - 처리량(TPS): {:.2f} requests/sec", withCache.throughput);
        log.info("   - 총 실행시간: {} ms", withCache.totalTime);
        log.info("   - 성공 요청: {}개", withCache.successCount);

        log.info("");
        log.info(" 개선 효과:");
        double responseTimeImprovement = ((withoutCache.averageResponseTime - withCache.averageResponseTime)
                / withoutCache.averageResponseTime) * 100;
        double throughputImprovement = ((withCache.throughput - withoutCache.throughput)
                / withoutCache.throughput) * 100;

        log.info("   - 응답시간 개선: {:.1f}% ({:.2f} ms → {:.2f} ms)",
                responseTimeImprovement, withoutCache.averageResponseTime, withCache.averageResponseTime);
        log.info("   - 처리량 증가: {:.1f}% ({:.2f} → {:.2f} TPS)",
                throughputImprovement, withoutCache.throughput, withCache.throughput);

        log.info("");
        log.info("=".repeat(60));
    }

    @Test
    @DisplayName("Redis 인기상품 랭킹 - 기본 기능 테스트")
    void testBasicPopularProductRanking() {
        log.info("\n=== Redis 인기상품 랭킹 기본 테스트 ===\n");

        Long product1Id = products.get(0).getId();
        Long product2Id = products.get(1).getId();
        Long product3Id = products.get(2).getId();

        simulatePurchase(product1Id, 10);
        simulatePurchase(product2Id, 5);
        simulatePurchase(product3Id, 3);

        List<ResponseProduct> dailyRanking = productService.getPopularProducts("daily", 5);

        assertThat(dailyRanking).hasSize(3);
        assertThat(dailyRanking.get(0).productId()).isEqualTo(product1Id);
        assertThat(dailyRanking.get(1).productId()).isEqualTo(product2Id);
        assertThat(dailyRanking.get(2).productId()).isEqualTo(product3Id);

        log.info("일간 랭킹 조회 성공:");
        for (int i = 0; i < dailyRanking.size(); i++) {
            log.info("{}위: {} (상품 ID: {})", i+1, dailyRanking.get(i).productName(), dailyRanking.get(i).productId());
        }
    }

    /**
     * 구매 시뮬레이션 - OrderEventHandler의 인기도 업데이트 로직 직접 호출
     */
    private void simulatePurchase(Long productId, int quantity) {
        String dailyKey = getDailyPopularKey();
        String weeklyKey = getWeeklyPopularKey();

        redisTemplate.opsForZSet().incrementScore(dailyKey, productId.toString(), quantity);
        redisTemplate.opsForZSet().incrementScore(weeklyKey, productId.toString(), quantity);

        redisTemplate.expire(dailyKey, java.time.Duration.ofDays(1));
        redisTemplate.expire(weeklyKey, java.time.Duration.ofDays(7));
    }

    @Test
    @DisplayName("Redis 인기상품 랭킹 - 주간 vs 일간 비교")
    void testWeeklyVsDailyRanking() {
        log.info("\n=== 주간 vs 일간 랭킹 비교 테스트 ===\n");

        Long product1Id = products.get(0).getId();
        Long product2Id = products.get(1).getId();
        Long product3Id = products.get(2).getId();

        simulatePurchase(product1Id, 8);
        simulatePurchase(product2Id, 12);
        simulatePurchase(product3Id, 5);

        List<ResponseProduct> dailyRanking = productService.getPopularProducts("daily", 5);
        List<ResponseProduct> weeklyRanking = productService.getPopularProducts("weekly", 5);

        assertThat(dailyRanking).hasSize(3);
        assertThat(weeklyRanking).hasSize(3);

        log.info("=========== 결과값 및 순위 정렬 ==============");
        String dailyKey = getDailyPopularKey();
        for(ResponseProduct responseProduct : dailyRanking) {
            Double redisScore = redisTemplate.opsForZSet().score(dailyKey, responseProduct.productId().toString());
            log.info("상품 ID: {}, 상품명: {}, DB 판매량: {}, Redis 점수: {}",
                    responseProduct.productId(),
                    responseProduct.productName(),
                    responseProduct.sellQuantity(),
                    redisScore != null ? redisScore.intValue() : 0);
        }

        assertThat(dailyRanking.get(0).productId()).isEqualTo(weeklyRanking.get(0).productId());
        assertThat(dailyRanking.get(0).productId()).isEqualTo(product2Id);

        log.info("일간 1위: {}", dailyRanking.get(0).productName());
        log.info("주간 1위: {}", weeklyRanking.get(0).productName());
    }

    @Test
    @DisplayName("Redis 인기상품 랭킹 - 동시 구매 테스트")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testConcurrentPurchases() throws InterruptedException {
        log.info("\n=== 동시 구매 인기상품 랭킹 테스트 ===\n");

        Long productId = products.get(0).getId();
        int totalPurchases = 100;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalPurchases);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < totalPurchases; i++) {
            executor.submit(() -> {
                try {
                    simulatePurchase(productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("구매 시뮬레이션 실패", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        String dailyKey = getDailyPopularKey();
        Double score = redisTemplate.opsForZSet().score(dailyKey, productId.toString());

        assertThat(score).isNotNull();
        assertThat(score.intValue()).isEqualTo(successCount.get());

        log.info("동시 구매 테스트 결과:");
        log.info("- 성공한 구매: {}회", successCount.get());
        log.info("- Redis 저장된 점수: {}", score);
        log.info("- 점수 일치 여부: {}", score.intValue() == successCount.get());
    }

    private String getDailyPopularKey() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "popular:daily:" + today;
    }

    private String getWeeklyPopularKey() {
        int weekOfYear = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        return "popular:weekly:" + LocalDate.now().getYear() + ":" + weekOfYear;
    }

    private void clearRedisData() {
        String dailyPattern = "popular:daily:*";
        String weeklyPattern = "popular:weekly:*";

        Set<String> dailyKeys = redisTemplate.keys(dailyPattern);
        Set<String> weeklyKeys = redisTemplate.keys(weeklyPattern);

        if (dailyKeys != null && !dailyKeys.isEmpty()) {
            redisTemplate.delete(dailyKeys);
            log.info("Redis 일간 키 삭제: {}", dailyKeys);
        }
        if (weeklyKeys != null && !weeklyKeys.isEmpty()) {
            redisTemplate.delete(weeklyKeys);
            log.info("Redis 주간 키 삭제: {}", weeklyKeys);
        }

        String todayDaily = getDailyPopularKey();
        String thisWeekly = getWeeklyPopularKey();
        redisTemplate.delete(todayDaily);
        redisTemplate.delete(thisWeekly);
        log.info("현재 테스트 키 삭제: {}, {}", todayDaily, thisWeekly);
    }

    private static class PerformanceResult {
        final double averageResponseTime;
        final double throughput;
        final long totalTime;
        final int successCount;

        PerformanceResult(double averageResponseTime, double throughput, long totalTime, int successCount) {
            this.averageResponseTime = averageResponseTime;
            this.throughput = throughput;
            this.totalTime = totalTime;
            this.successCount = successCount;
        }
    }
}
