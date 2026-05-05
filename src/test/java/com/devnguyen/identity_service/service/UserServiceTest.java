package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.entity.Role;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.mapper.UserMapper;
import com.devnguyen.identity_service.repository.RoleRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 * @ExtendWith(MockitoExtension.class):
 * Bật Mockito JUnit 5 Extension.
 * Extension này tự động:
 * → Khởi tạo các @Mock object trước mỗi test
 * → Inject @Mock vào @InjectMocks
 * → Reset mock sau mỗi test (tránh state leak giữa các test)
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    /*
     * @Mock: Tạo mock object — object giả không có logic thật.
     * Mọi method của mock mặc định trả về:
     * → null cho Object
     * → 0 cho số
     * → false cho boolean
     * → empty collection cho List/Set
     * Bạn dùng when().thenReturn() để override behavior
     */
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    /*
     * @InjectMocks: Tạo UserService THẬT (không phải mock).
     * Mockito tự inject tất cả @Mock ở trên vào UserService
     * thông qua constructor injection (vì UserService dùng @RequiredArgsConstructor).
     *
     * Kết quả: UserService thật nhưng dependencies đều là mock
     * → Test được business logic mà không cần DB, không cần Spring context
     */
    @InjectMocks
    private UserService userService;

    // ===== Test data dùng chung =====
    private User mockUser;
    private UserResponse mockUserResponse;
    private UserCreationRequest creationRequest;

    /*
     * @BeforeEach: Chạy trước MỖI test case.
     * Tại sao setup ở đây thay vì trong từng test?
     * → DRY (Don't Repeat Yourself): không lặp code setup
     * → Mỗi test bắt đầu với state sạch (object mới hoàn toàn)
     */
    @BeforeEach
    void setUp() {
        // Tạo mock User entity
        mockUser = User.builder()
                .id("test-uuid-123")
                .username("john")
                .password("encoded_password")
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(1995, 6, 15))
                .roles(Set.of(Role.builder()
                        .name("USER")
                        .description("User role")
                        .build()))
                .build();

        // Tạo mock UserResponse DTO
        mockUserResponse = UserResponse.builder()
                .id("test-uuid-123")
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .dob(LocalDate.of(1995, 6, 15))
                .build();

        // Tạo request object
        creationRequest = new UserCreationRequest();
        creationRequest.setUsername("john");
        creationRequest.setPassword("password123");
        creationRequest.setFirstName("John");
        creationRequest.setLastName("Doe");
        creationRequest.setDob(LocalDate.of(1995, 6, 15));
    }

    // ==================== createUser Tests ====================

    /*
     * Test naming convention: methodName_condition_expectedResult
     * Đọc tên test là biết ngay: "createUser khi user đã tồn tại thì throw exception"
     */
    @Test
    void createUser_userAlreadyExists_throwsUserExistedException() {
        // ARRANGE
        /*
         * Khi gọi existsByUsername("john") → trả về true (user đã tồn tại)
         * anyString(): match bất kỳ String nào
         * Dùng anyString() thay vì "john" cụ thể để test tổng quát hơn
         */
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // ACT & ASSERT
        /*
         * assertThrows(ExceptionClass, () -> { code ném exception })
         * → Kiểm tra lambda có throw đúng exception không
         * → Nếu không throw hoặc throw exception khác → test FAIL
         *
         * Tại sao không dùng try-catch?
         * → assertThrows rõ ràng hơn, ít code hơn
         * → JUnit tự handle việc catch và verify
         */
        AppException exception = assertThrows(AppException.class,
                () -> userService.createUser(creationRequest));

        // Kiểm tra đúng ErrorCode
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_EXISTED);

        /*
         * verify(): kiểm tra mock có được gọi hay không.
         * verify(userRepository, never()).save(any()):
         * → Đảm bảo save() KHÔNG được gọi khi user đã tồn tại
         * → Business logic đúng: không save user trùng username
         */
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_validRequest_returnsUserResponse() {
        // ARRANGE
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userMapper.toUser(any(UserCreationRequest.class))).thenReturn(mockUser);
        /*
         * passwordEncoder.encode() trả về chuỗi encoded giả
         * Không dùng BCrypt thật vì:
         * → Chậm (BCrypt tốn ~100ms)
         * → Không cần test BCrypt hoạt động đúng (đó là việc của BCrypt library)
         * → Chỉ cần test UserService gọi encode() và set vào user
         */
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(roleRepository.findAllById(any())).thenReturn(List.of(
                Role.builder().name("USER").build()
        ));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mockUserResponse);

        // ACT
        UserResponse result = userService.createUser(creationRequest);

        // ASSERT
        /*
         * assertThat() từ AssertJ library — fluent API rất dễ đọc:
         * assertThat(actual).isNotNull()
         * assertThat(actual).isEqualTo(expected)
         * assertThat(actual).contains(...)
         */
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getId()).isEqualTo("test-uuid-123");

        /*
         * verify(passwordEncoder).encode("password123"):
         * Đảm bảo encode() được gọi đúng 1 lần với đúng argument
         * → Test business rule: password PHẢI được encode trước khi save
         */
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    // ==================== getUser Tests ====================

    @Test
    void getUser_userExists_returnsUserResponse() {
        // ARRANGE
        when(userRepository.findById("test-uuid-123"))
                .thenReturn(Optional.of(mockUser));
        when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

        // ACT
        UserResponse result = userService.getUser("test-uuid-123");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-uuid-123");
        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void getUser_userNotFound_throwsUserNotExistedException() {
        // ARRANGE
        /*
         * Optional.empty(): simulate trường hợp không tìm thấy user trong DB
         */
        when(userRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        AppException exception = assertThrows(AppException.class,
                () -> userService.getUser("non-existent-id"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED);
    }

    // ==================== updateUser Tests ====================

    @Test
    void updateUser_userExists_updatesSuccessfully() {
        // ARRANGE
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setPassword("newpassword123");

        when(userRepository.findById("test-uuid-123"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newpassword123"))
                .thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mockUserResponse);

        // ACT
        UserResponse result = userService.updateUser("test-uuid-123", updateRequest);

        // ASSERT
        assertThat(result).isNotNull();

        // Đảm bảo password mới được encode
        verify(passwordEncoder).encode("newpassword123");
        // Đảm bảo save được gọi để persist thay đổi
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_userNotFound_throwsException() {
        // ARRANGE
        when(userRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        AppException exception = assertThrows(AppException.class,
                () -> userService.updateUser("wrong-id", new UserUpdateRequest()));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED);
        // Không được save khi user không tồn tại
        verify(userRepository, never()).save(any());
    }

    // ==================== deleteUser Tests ====================

    @Test
    void deleteUser_userExists_deletesSuccessfully() {
        // ARRANGE
        when(userRepository.existsById("test-uuid-123")).thenReturn(true);

        // ACT
        userService.deleteUser("test-uuid-123");

        // ASSERT
        /*
         * verify(userRepository, times(1)).deleteById("test-uuid-123"):
         * Đảm bảo deleteById được gọi đúng 1 lần với đúng ID
         * times(1) là default, có thể bỏ qua nhưng viết tường minh cho rõ
         */
        verify(userRepository, times(1)).deleteById("test-uuid-123");
    }

    @Test
    void deleteUser_userNotFound_throwsException() {
        // ARRANGE
        when(userRepository.existsById(anyString())).thenReturn(false);

        // ACT & ASSERT
        AppException exception = assertThrows(AppException.class,
                () -> userService.deleteUser("non-existent-id"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED);
        // Không được gọi deleteById khi user không tồn tại
        verify(userRepository, never()).deleteById(anyString());
    }

    // ==================== getUsers Tests ====================

    @Test
    void getUsers_returnsAllUsers() {
        // ARRANGE
        User anotherUser = User.builder()
                .id("uuid-456")
                .username("jane")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(mockUser, anotherUser));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mockUserResponse);

        // ACT
        var result = userService.getUsers();

        // ASSERT
        /*
         * hasSize(2): kiểm tra list có đúng 2 phần tử
         * Đảm bảo findAll() được gọi và tất cả user được map
         */
        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }
}