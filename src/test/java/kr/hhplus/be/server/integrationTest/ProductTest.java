package kr.hhplus.be.server.integrationTest;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.enums.ProductStatus;
import kr.hhplus.be.server.repository.ProductRepository;
import kr.hhplus.be.server.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class ProductTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private List<Product> products;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

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

}
