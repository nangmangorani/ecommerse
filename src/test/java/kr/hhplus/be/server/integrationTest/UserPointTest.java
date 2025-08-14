package kr.hhplus.be.server.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.point.RequestPointCharge;
import kr.hhplus.be.server.enums.UserStatus;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.service.PointHistService;
import kr.hhplus.be.server.service.UserService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public class UserPointTest {

    /**
     * 사용자 조회 및 포인트 충전 통합테스트
     */

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private PointHistService pointHistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User("테스트유저1", UserStatus.ACTIVE, 5000L);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("사용자가 존재하지 않음")
    void noUserTest() throws Exception {

        Long id = 999L;

        mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("사용자 조회 성공")
    void successGetUser() throws Exception {

        mockMvc.perform(get("/point/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.userName").value(testUser.getName()))
                .andExpect(jsonPath("$.userPoint").value(testUser.getPoint()));

    }

    @Test
    @DisplayName("포인트가 음수값이 들어와 충전되지 않음")
    void inputPointMinus() throws Exception {

        RequestPointCharge request = new RequestPointCharge(testUser.getId(), -1000L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("포인트는 음수일 수 없음"));

    }

    @Test
    @DisplayName("포인트 충전 성공")
    void successChargePoint() throws Exception {

        RequestPointCharge request = new RequestPointCharge(testUser.getId(), 1000L);

        String requestBodyJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.userName").value(testUser.getName()))
                .andExpect(jsonPath("$.userPoint").value(testUser.getPoint()));
    }
}
