package com.devnguyen.identity_service.controller;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @SpringBootTest: Load toàn bộ Spring Application Context.
 * Khác với @ExtendWith(MockitoExtension) — cái đó không có Spring context.
 *
 * Tại sao cần Spring context cho Controller test?
 * → Controller test cần: Security Filter Chain, DispatcherServlet,
 *   Jackson serialization, Exception Handler... tất cả đều cần Spring
 * → Không thể mock tất cả những thứ này bằng Mockito thuần
 *
 * @AutoConfigureMockMvc: tự động tạo MockMvc Bean.
 * MockMvc: giả lập HTTP request mà không cần chạy server thật.
 * → Nhanh hơn chạy server thật
 * → Không cần port, không cần mở connection
 *
 * @TestPropertySource: override properties cho test.
 * → Dùng test DB khác production DB
 * → Inject JWT key cho test
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.signerKey=29da87f839479eab94bb191c6f21f37127a5b6ba3c4a6c01593d91f05905019f",
        "jwt.valid-duration=3600",
        "jwt.refreshable-duration=36000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /*
     * @MockBean: tạo mock và đăng ký vào Spring context.
     * Khác @Mock: @Mock chỉ dùng trong Mockito context (không có Spring).
     * @MockBean: dùng khi có @SpringBootTest — thay thế Bean thật trong context.
     *
     * UserService thật sẽ gọi DB → không muốn vậy trong test
     * → MockBean thay thế UserService thật bằng mock
     */
    @MockBean
    private UserService userService;

    private ObjectMapper objectMapper;
    private UserCreationRequest validRequest;
    private UserResponse mockResponse;

    @BeforeEach
    void setUp() {
        /*
         * ObjectMapper: serialize Java object → JSON string để gửi trong request body.
         * JavaTimeModule: support LocalDate serialization
         * (LocalDate không được support mặc định bởi Jackson)
         */
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validRequest = new UserCreationRequest();
        validRequest.setUsername("john");
        validRequest.setPassword("password123");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
        validRequest.setDob(LocalDate.of(1995, 6, 15));

        mockResponse = UserResponse.builder()
                .id("test-uuid-123")
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    @Test
    void createUser_validRequest_returns200WithUserResponse() throws Exception {
        // ARRANGE
        when(userService.createUser(any(UserCreationRequest.class)))
                .thenReturn(mockResponse);

        // ACT & ASSERT
        /*
         * mockMvc.perform(): thực hiện HTTP request giả
         * post("/users"): HTTP POST đến /users
         *   (context-path "/identity" được add tự động bởi @SpringBootTest)
         *
         * .contentType(APPLICATION_JSON): set Content-Type header
         * .content(json): request body dạng JSON string
         *
         * .andExpect(): assert response
         * status().isOk(): HTTP 200
         * jsonPath("$.code"): đọc field "code" từ JSON response
         * jsonPath("$.result.username"): đọc nested field
         */
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.username").value("john"))
                .andExpect(jsonPath("$.result.id").value("test-uuid-123"));
    }

    @Test
    void createUser_usernameTooShort_returns400() throws Exception {
        // ARRANGE: username chỉ có 2 ký tự → vi phạm @Size(min=3)
        validRequest.setUsername("ab");

        // ACT & ASSERT
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1003));
        // UserService không được gọi khi validation fail
    }

    @Test
    void createUser_passwordTooShort_returns400() throws Exception {
        // ARRANGE
        validRequest.setPassword("short");

        // ACT & ASSERT
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void createUser_userAlreadyExists_returns400() throws Exception {
        // ARRANGE: Service throw AppException
        when(userService.createUser(any(UserCreationRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_EXISTED));

        // ACT & ASSERT
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
//                .andExpect(jsonPath("$.message").value("User already existed"));
    }
}