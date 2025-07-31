package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.exception.custom.CustomException;
import kr.hhplus.be.server.repository.ProductRepository;
import org.springframework.stereotype.Service;

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

        String status = "01";

        List<Product> products = productRepository.findByStatus(status);

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

    public Product getProductInfo(long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("상품이 존재하지 않음"));

        if (product.getQuantity() <= 0) {
            throw new CustomException("상품 재고 부족");
        }

        return product;
    }

}
