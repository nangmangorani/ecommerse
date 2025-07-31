package kr.hhplus.be.server.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.*;
import kr.hhplus.be.server.dto.order.RequestOrder;
import kr.hhplus.be.server.repository.*;
import kr.hhplus.be.server.service.OrderService;
import kr.hhplus.be.server.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class OrderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product;

    private User user;

    private Order order;

    private Payment payment;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        userRepository.deleteAll();

//        user = new User("테스트유저1", "01", 5000L);

        product = new Product("iPhone 15", "01", 1, 25, 1200000L, "전자제품");

        coupon = new Coupon("쿠폰1", "01", 20, 10, 3, 1L);

        product = productRepository.save(product);
        coupon = couponRepository.save(coupon);
    }

    @Test
    @DisplayName("사용자 잔고 부족")
    void hasSufficientBalance() throws Exception {

        user = new User("테스트유저1", "01", 5000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1500000L,1200000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("잔고 부족"));
    }

    @Test
    @DisplayName("요청수량보다 재고 부족")
    void hasSufficientQuantity() throws Exception {
        user = new User("테스트유저1", "01", 5000000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 2, 3000000L,2400000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청수량보다 재고 부족"));

    }

    @Test
    @DisplayName("결제 성공")
    void successPayment() throws Exception {
        user = new User("테스트유저1", "01", 5000000L);
        User returnUser = userRepository.save(user);

        RequestOrder request = new RequestOrder(returnUser.getId(), product.getId(), coupon.getId(), 1, 1500000L,1200000L, true);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value(returnUser.getName()))
                .andExpect(jsonPath("$.couponYn").value(request.couponYn()))
                .andExpect(jsonPath("$.originalPrice").value(request.originalPrice()))
                .andExpect(jsonPath("$.discountPrice").value(request.requestPrice()));

    }

}
