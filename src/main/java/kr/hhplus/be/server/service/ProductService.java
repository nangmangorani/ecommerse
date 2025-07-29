package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.product.ResponseProduct;
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
        List<Product> products = productRepository.findAll();

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
                .orElseThrow(() -> new RuntimeException());    }

}
