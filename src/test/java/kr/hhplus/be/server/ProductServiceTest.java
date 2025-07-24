package kr.hhplus.be.server;


import kr.hhplus.be.domain.Product;
import kr.hhplus.be.dto.product.ResponseProduct;
import kr.hhplus.be.repository.ProductRepository;
import kr.hhplus.be.service.ProductService;
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
import static org.mockito.BDDMockito.*;

public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // @Mock, @InjectMocks 어노테이션 필드 초기화
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

        // given
        given(productRepository.getProductList()).willReturn(Collections.emptyList());

        // when
        List<ResponseProduct> response = productService.getProductList();

        // then
        assertThat(response).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("상품 목록 정상적으로 조회")
    void 상품_목록_정상적으로_조회() {

        List<Product> products = Arrays.asList(
                new Product(1, "상품1", "Y", 10, 1, 1000L, "침구류"),
                new Product(2, "상품2", "Y", 20, 1, 2000L, "침구류"),
                new Product(3, "상품3", "Y", 13, 3, 2100L, "침구류")
        );

        // given
        given(productRepository.getProductList()).willReturn(products);

        // when
        List<ResponseProduct> response = productService.getProductList();

        // then
        assertThat(response).hasSize(3);
        assertThat(response.get(0).productName()).isEqualTo("상품1");
        assertThat(response.get(1).price()).isEqualTo(2000L);
        assertThat(response.get(2).productQuantity()).isEqualTo(13);
    }

    /**
     * 상위 상품목록 조회 테스트
     * 1. 상품이 없을 경우 빈 리스트 반환
     * 2. 상품이 5개 미만일 경우
     */

    @Test
    @DisplayName("상품이 없을 경우")
    void 상품이_없을_경우() {

        // given
        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(Collections.emptyList());

        // when
        List<ResponseProduct> response = productService.getProductList();

        // then
        assertThat(response).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("상품이 5개 미만일 경우")
    void 상품이_5개_미만일_경우() {

        List<Product> products = Arrays.asList(
                new Product(1, "상품1", "Y", 10, 10, 1000L, "침구류"),
                new Product(2, "상품2", "Y", 20, 11, 2000L, "문구류"),
                new Product(3, "상품3", "Y", 13, 12, 2100L, "의류")
        );

        // given
        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(products);

        // when
        List<ResponseProduct> response = productService.getProductListTop5();

        // then
        assertThat(response).hasSize(3);
        assertThat(response.get(0).productName()).isEqualTo("상품1");
        assertThat(response.get(1).price()).isEqualTo(2000L);
        assertThat(response.get(2).productQuantity()).isEqualTo(13);
    }

    @Test
    @DisplayName("상위 5개 상품 정상적으로 조회")
    void 상위_5개상품_정상적으로_조회() {
        List<Product> products = Arrays.asList(
                new Product(1, "상품1", "Y", 10, 10, 1000L, "침구류"),
                new Product(2, "상품2", "Y", 20, 11, 2000L, "문구류"),
                new Product(3, "상품3", "Y", 13, 12, 2100L, "의류"),
                new Product(4, "상품4", "Y", 14, 10, 3000L, "침구류"),
                new Product(5, "상품5", "Y", 15, 11, 1800L, "문구류")
        );

        // given
        given(productRepository.findTop5ByOrderBySellQuantityDesc()).willReturn(products);

        // when
        List<ResponseProduct> response = productService.getProductListTop5();

        // then
        assertThat(response).hasSize(5);
        assertThat(response.get(0).productName()).isEqualTo("상품1");
        assertThat(response.get(1).price()).isEqualTo(2000L);
        assertThat(response.get(2).productQuantity()).isEqualTo(13);
    }

    /**
     * 상품상세조회 테스트케이스
     * 1. 상품이 존재하지 않을 경우
     * 2.
     */

    @Test
    @DisplayName("상품이 존재하지 않을 경우")
    void 상품이_존재하지_않을_경우() {

        long id = 1;

        // given
        given(productRepository.findById(1)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> productService.getProduct(id))
                .isInstanceOf(RuntimeException.class);

    }

    @Test
    @DisplayName("상품이 정상적으로 조회될 경우")
    void 상품이_정상적으로_조회될_경우() {

        long id = 1;

        Product product = new Product(
                1, "상품1", "Y",10, 9,1000L, "문구류"
        );

        // given
        given(productRepository.findById(id)).willReturn(Optional.of(product));

        // when
        ResponseProduct result = productService.getProduct(id);

        //then
        assertThat(result.productId()).isEqualTo(product.getId());
        assertThat(result.productName()).isEqualTo(product.getName());
        assertThat(result.productQuantity()).isEqualTo(product.getQuantity());
        assertThat(result.price()).isEqualTo(product.getPrice());
        assertThat(result.productType()).isEqualTo(product.getProductType());

    }



}
