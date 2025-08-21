package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 상품리스트 조회
     * @return List<ResponseProductList>
     */
    public List<ResponseProduct> getProductList() {

        List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE);

        return products.stream()
                .map(ResponseProduct::from)
                .toList();
    }

    public ResponseProduct getProduct(long id) {

        Optional<Product> product = productRepository.findById(id);

        return product
                .map(ResponseProduct::from)
                .orElseThrow(() -> new CustomException("상품이 존재하지 않음"));
    }

    /**
     * 판매상품 상위 5개 조회
     * Cache-Aside 패턴 사용
     * @return List<ResponseProductList>
     */
    @Cacheable(value = "top5-products", key = "'sellQuantity'")
    public List<ResponseProduct> getProductListTop5() {
        return getTop5ProductsFromDB();
    }


    public Product getProductInfo(RequestOrder requestOrder) {

        Product product = productRepository.findByIdAndStatus(requestOrder.productId(), ProductStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("상품이 존재하지 않음"));

        product.checkPrice(requestOrder.originalPrice(), product.getPrice() * requestOrder.requestQuantity());

        product.checkQuantity(requestOrder.requestQuantity(), product.getQuantity());

        return product;
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new CustomException("상품을 찾을 수 없습니다"));

        product.decreaseStock(quantity);
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("상품을 찾을 수 없습니다"));

        product.increaseStock(quantity);
    }

    private ResponseProduct getProductFromDB(Long productId) {
        return productRepository.findById(productId)
                .map(ResponseProduct::from)
                .orElse(null);
    }

    private List<ResponseProduct> getTop5ProductsFromDB() {
        List<Product> products = productRepository.findTop5ByOrderBySellQuantityDesc();

        if (products == null) {
            products = List.of();
        }

        return products.stream()
                .map(ResponseProduct::from)
                .toList();
    }

    public List<ResponseProduct> getPopularProducts(String period, int limit) {

        String key = getPopularProductKey(period);

        Set<ZSetOperations.TypedTuple<Object>> topProducts =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit -1);

        if (topProducts == null || topProducts.isEmpty()) {
            log.info("Redis에 {}기간 인기상품 데이터 없음 - 기존 TOP5 로직으로 대체", period);
            return getTop5ProductsFromDB();
        }
        List<Long> productIds = topProducts.stream()
                .map(tuple -> {
                    log.debug("상품 ID: {}, 점수: {}", tuple.getValue(), tuple.getScore());
                    return Long.parseLong(tuple.getValue().toString());
                })
                .toList();

        List<Product> products = productRepository.findByIdInAndStatus(productIds, ProductStatus.ACTIVE);

        return productIds.stream()
                .map(id -> products.stream()
                        .filter(p -> p.getId().equals(id))
                        .findFirst()
                        .map(ResponseProduct::from)
                        .orElse(null))
                .filter(product -> product != null)
                .toList();
    }

    private String getPopularProductKey(String period) {
        if ("weekly".equals(period)) {
            int weekOfYear = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            int year = LocalDate.now().getYear();
            return "popular:weekly:" + year + ":" + weekOfYear;
        } else if ("daily".equals(period)) {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return "popular:daily:" + today;
        }

        throw new CustomException("지원하지 않는 기간: " + period);
    }

    /**
     * TOP5 캐시 무효화
     */
    @CacheEvict(value = "top5-products", key = "'sellQuantity'")
    public void invalidateTop5Cache() {
        log.info("TOP5 캐시 무효화");
    }

}
