package kr.hhplus.be.server;


import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.product.ResponseProduct;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.*;

public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

    }

    /**
     * 상품목록 조회 테스트
     * 1. 상품목록이 없을 경우 -> 예외나면안됨
     * 2. 정상조회
     */

    @Test
    @DisplayName("상품 목록이 없을 경우")
    void 상품_목록이_없을_경우() {

        given(productRepository.findAll()).willReturn(Collections.emptyList());

        List<ResponseProduct> response = productService.getProductList();

        assertAll("빈 상품 목록 검증",
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response).isEmpty()
        );
    }

    @Test
    @DisplayName("상품 목록 정상적으로 조회")
    void 상품_목록_정상적으로_조회() {

        List<Product> products = Arrays.asList(
                new Product(1, "상품1", ProductStatus.ACTIVE, 10, 1, 1000L, "침구류"),
                new Product(2, "상품2", ProductStatus.ACTIVE, 20, 1, 2000L, "침구류"),
                new Product(3, "상품3", ProductStatus.ACTIVE, 13, 3, 2100L, "침구류")
        );

        given(productRepository.findByStatus(ProductStatus.ACTIVE)).willReturn(products);

        List<ResponseProduct> response = productService.getProductList();

        assertAll("상품 목록 조회 결과 검증",
                () -> assertThat(response).hasSize(3),
                () -> assertThat(response.get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(response.get(1).price()).isEqualTo(2000L),
                () -> assertThat(response.get(2).productQuantity()).isEqualTo(13)
        );
    }

    /**
     * 상위 상품목록 조회 테스트
     * 1. 상품이 없을 경우 빈 리스트 반환
     * 2. 상품이 5개 미만일 경우
     */

    @Test
    @DisplayName("상품이 없을 경우")
    void 상품이_없을_경우() {

        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(Collections.emptyList());

        List<ResponseProduct> response = productService.getProductList();

        assertAll("빈 TOP5 상품 목록 검증",
                () -> assertThat(response).isNotNull(),
                () -> assertThat(response).isEmpty()
        );
    }

    @Test
    @DisplayName("상품이 5개 미만일 경우")
    void 상품이_5개_미만일_경우() {

        List<Product> products = Arrays.asList(
                new Product(1, "상품1", ProductStatus.ACTIVE, 10, 10, 1000L, "침구류"),
                new Product(2, "상품2", ProductStatus.ACTIVE, 20, 11, 2000L, "문구류"),
                new Product(3, "상품3", ProductStatus.ACTIVE, 13, 12, 2100L, "의류")
        );

        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(products);

        List<ResponseProduct> response = productService.getProductListTop5();

        assertAll("5개 미만 상품 조회 결과 검증",
                () -> assertThat(response).hasSize(3),
                () -> assertThat(response.get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(response.get(1).price()).isEqualTo(2000L),
                () -> assertThat(response.get(2).productQuantity()).isEqualTo(13),
                () -> assertThat(response.get(0).sellQuantity()).isEqualTo(10),
                () -> assertThat(response.get(1).sellQuantity()).isEqualTo(11),
                () -> assertThat(response.get(2).sellQuantity()).isEqualTo(12)
        );
        }

    @Test
    @DisplayName("상위 5개 상품 정상적으로 조회")
    void 상위_5개상품_정상적으로_조회() {
        List<Product> products = Arrays.asList(
                new Product(1, "상품1", ProductStatus.ACTIVE, 10, 10, 1000L, "침구류"),
                new Product(2, "상품2", ProductStatus.ACTIVE, 20, 11, 2000L, "문구류"),
                new Product(3, "상품3", ProductStatus.ACTIVE, 13, 12, 2100L, "의류"),
                new Product(4, "상품4", ProductStatus.ACTIVE, 14, 10, 3000L, "침구류"),
                new Product(5, "상품5", ProductStatus.ACTIVE, 15, 11, 1800L, "문구류")
        );

        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(products);

        List<ResponseProduct> response = productService.getProductListTop5();

        assertAll("TOP5 상품 조회 결과 검증",
                () -> assertThat(response).hasSize(5),
                () -> assertThat(response.get(0).productName()).isEqualTo("상품1"),
                () -> assertThat(response.get(1).price()).isEqualTo(2000L),
                () -> assertThat(response.get(2).productQuantity()).isEqualTo(13),
                () -> assertThat(response.get(3).productName()).isEqualTo("상품4"),
                () -> assertThat(response.get(4).price()).isEqualTo(1800L)
        );

    }

    /**
     * 상품상세조회 테스트케이스
     * 1. 상품이 존재하지 않을 경우
     * 2.
     */

    @Test
    @DisplayName("상품이 존재하지 않을 경우")
    void 상품이_존재하지_않을_경우() {

        Long id = 1L;

        given(productRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(id))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @DisplayName("상품이 정상적으로 조회될 경우")
    void 상품이_정상적으로_조회될_경우() {

        long id = 1;

        Product product = new Product(
                1, "상품1", ProductStatus.ACTIVE,10, 9,1000L, "문구류"
        );

        given(productRepository.findById(id)).willReturn(Optional.of(product));

        ResponseProduct result = productService.getProduct(id);

        assertAll("상품 상세 조회 결과 검증",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.productId()).isEqualTo(product.getId()),
                () -> assertThat(result.productName()).isEqualTo(product.getName()),
                () -> assertThat(result.productQuantity()).isEqualTo(product.getQuantity()),
                () -> assertThat(result.sellQuantity()).isEqualTo(product.getSellQuantity()),
                () -> assertThat(result.price()).isEqualTo(product.getPrice()),
                () -> assertThat(result.productType()).isEqualTo(product.getType())
        );
    }

}
