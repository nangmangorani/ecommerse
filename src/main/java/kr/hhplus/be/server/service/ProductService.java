package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductCacheService cacheService;

    public ProductService(ProductRepository productRepository, RedisTemplate<String, Object> redisTemplate, ProductCacheService cacheService) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.cacheService = cacheService;
    }

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
    public List<ResponseProduct> getProductListTop5() {

        long startTime = System.currentTimeMillis();

        try{
            // 1. 캐시에서 먼저 조회
            List<ResponseProduct> cachedProducts = cacheService.getTop5Products();

            if (cachedProducts != null && !cachedProducts.isEmpty()) {
                cacheService.recordCacheHit();
                long duration = System.currentTimeMillis() - startTime;
                log.info("TOP5 상품 조회 완료 (캐시 히트), 응답시간: {}ms", duration);
                return cachedProducts;
            }

            // 2. 캐시 미스 시 DB 조회
            cacheService.recordCacheMiss();
            log.info("TOP5 상품 캐시 미스 - DB 조회 시작");

            List<ResponseProduct> products = getTop5ProductsFromDB();

            // 3. 캐시에 저장
            cacheService.setTop5Products(products);

            long duration = System.currentTimeMillis() - startTime;
            log.info("TOP5 상품 조회 완료 (DB), 응답시간: {}ms", duration);

            return products;
        } catch(Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("TOP5 상품 조회 중 오류 발생, 응답시간: {}ms", duration, e);
            // 오류 시 DB 직접 조회
            return getTop5ProductsFromDB();
        }
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

    // 재고 증가 (롤백용)
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
        List<Product> products = Optional.ofNullable(
                productRepository.findTop5ByOrderBySellQuantityDesc()
        ).orElseGet(List::of);

        return products.stream()
                .map(ResponseProduct::from)
                .toList();
    }
}
