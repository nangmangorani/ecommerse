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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    /**
     * TOP5 캐시 무효화
     */
    @CacheEvict(value = "top5-products", key = "'sellQuantity'")
    public void invalidateTop5Cache() {
        log.info("TOP5 캐시 무효화");
    }

    @CacheEvict(value = "top5-products", key = "'sellQuantity'")
    @Transactional
    public void updateSellQuantity(Long productId, int soldQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("상품 없음"));

        product.increaseStock(soldQuantity);
        log.info("상품 {} 판매량 업데이트 + 캐시 무효화", productId);
    }

}
