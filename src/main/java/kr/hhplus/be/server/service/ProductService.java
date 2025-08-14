package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

    /**
     * 판매상품 상위 5개 조회
     * @return List<ResponseProductList>
     */
    public List<ResponseProduct> getProductListTop5() {

        List<Product> products = Optional.ofNullable(productRepository.findTop5ByOrderBySellQuantityDesc())
                .orElseGet(List::of);
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
}
