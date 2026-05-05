package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.enums.PredefinedRole;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.mapper.UserMapper;
import com.devnguyen.identity_service.repository.RoleRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /*
     * @Transactional: Toàn bộ method chạy trong 1 transaction.
     * Tại sao cần ở đây?
     * → Có 2 thao tác DB: save user + set roles
     * → Nếu save user OK nhưng set roles lỗi → rollback toàn bộ
     * → Tránh trạng thái không nhất quán: user tồn tại nhưng không có role
     */
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        /*
         * Bước 1: Check duplicate username TRƯỚC khi làm gì khác.
         * Tại sao không để DB tự báo lỗi duplicate (unique constraint)?
         * → DB sẽ throw DataIntegrityViolationException → 500 Internal Server Error
         * → Không phải lỗi server! Là lỗi của client (409 Conflict / 400 Bad Request)
         * → Check trước → throw AppException với message rõ ràng → 400 Bad Request
         * → UX tốt hơn: "Username already existed" thay vì "Internal Server Error"
         */
        if (userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        // Bước 2: Map request → entity
        User user = userMapper.toUser(request);

        /*
         * Bước 3: Encode password TRƯỚC khi save.
         * KHÔNG BAO GIỜ lưu plain text password!
         * passwordEncoder.encode(): BCrypt hash với random salt
         * BCrypt tự generate salt mới mỗi lần → cùng password nhưng hash khác nhau
         * → Chống rainbow table attack
         */
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        /*
         * Bước 4: Gán role mặc định USER.
         * Tại sao gán role USER cho mọi user mới?
         * → Tất cả user đều cần có ít nhất 1 role để hệ thống phân quyền hoạt động
         * → USER là role tối thiểu, không có quyền đặc biệt
         *
         * Tại sao check request.getRoles() trước?
         * → Nếu request có gửi roles (admin tạo user với role cụ thể) → dùng roles đó
         * → Nếu không gửi roles (user tự register) → gán USER mặc định
         */
        var roles = new HashSet<>(roleRepository.findAllById(
                request.getRoles() != null
                        ? request.getRoles()
                        : List.of(PredefinedRole.USER_ROLE)
        ));
        user.setRoles(roles);

        // Bước 5: Save → DB tự generate UUID và set vào entity
        user = userRepository.save(user);

        log.info("User created: {}", user.getUsername());

        // Bước 6: Map entity → response DTO (không có password)
        return userMapper.toUserResponse(user);
    }

    /*
     * @PreAuthorize("hasAuthority('ROLE_ADMIN')")
     * → Chỉ user có authority "ROLE_ADMIN" mới gọi được method này.
     * → Check TRƯỚC khi method chạy → nếu không có quyền → 403 ngay.
     *
     * Tại sao "ROLE_ADMIN" có prefix "ROLE_" còn permission thì không?
     * → Spring Security convention: Role được prefix ROLE_ để phân biệt với Permission
     * → hasRole("ADMIN") tự thêm ROLE_ prefix
     * → hasAuthority("ROLE_ADMIN") check exact string
     * → Trong project này mình dùng hasAuthority() cho nhất quán
     *
     * Mình sẽ build scope JWT theo format: "ROLE_ADMIN ROLE_USER CREATE_DATA READ_DATA"
     * → Spring Security parse scope thành các GrantedAuthority
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("Getting all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    /*
     * @PostAuthorize: Check SAU khi method chạy.
     * "returnObject.username == authentication.name":
     *   returnObject = UserResponse trả về từ method
     *   authentication.name = username của user đang login (từ JWT)
     *
     * Ý nghĩa: User chỉ xem được thông tin của CHÍNH MÌNH.
     * ADMIN cũng có thể xem (vì authentication.name = "admin" và có thể
     * returnObject.username = "admin" nếu admin xem info của mình).
     *
     * Tại sao không dùng @PreAuthorize ở đây?
     * → Điều kiện phụ thuộc vào kết quả (username của user được query)
     * → Phải lấy data ra trước mới biết user này có phải của mình không
     * → @PreAuthorize không biết kết quả trước khi method chạy
     *
     * Cải tiến: Thêm OR để ADMIN cũng xem được:
     * "returnObject.username == authentication.name or hasAuthority('ROLE_ADMIN')"
     */
    @PostAuthorize("returnObject.username == authentication.name or hasAuthority('ROLE_ADMIN')")
    public UserResponse getUser(String id) {
        log.info("Getting user: {}", id);
        return userMapper.toUserResponse(
                userRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))
        );
    }

    /*
     * getMyInfo: Lấy thông tin của chính user đang login.
     * Không cần id parameter — lấy username từ SecurityContext.
     *
     * SecurityContextHolder: nơi Spring Security lưu thông tin
     * của user đang authenticate trong current thread.
     * → Thread-local → mỗi request có SecurityContext riêng → thread-safe
     *
     * Tại sao có method này khi đã có getUser(id)?
     * → User không biết id của mình (UUID dài, khó nhớ)
     * → "/users/myInfo" intuitive hơn "/users/abc-123-def-..."
     * → Không cần @PostAuthorize vì chắc chắn là của chính họ
     */
    public UserResponse getMyInfo() {
        /*
         * getContext().getAuthentication().getName():
         * → getContext(): lấy SecurityContext của thread hiện tại
         * → getAuthentication(): lấy Authentication object (chứa thông tin user đã login)
         * → getName(): lấy username (chính là "sub" trong JWT)
         */
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        /*
         * Tìm user trước, nếu không có → throw 404.
         * Không check username ở đây vì update không đổi username.
         */
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        /*
         * userMapper.updateUser(@MappingTarget user, request):
         * → Thay vì tạo User mới, MapStruct UPDATE object user hiện tại
         * → Chỉ update field không null trong request (IGNORE_NULL)
         * → Giữ nguyên id, username, và các field không có trong request
         */
        userMapper.updateUser(user, request);

        /*
         * Encode password mới nếu có.
         * Tại sao check != null?
         * → Update request có thể không có password (chỉ đổi tên)
         * → Không encode null → không set password mới
         */
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật roles nếu request có gửi
        if (request.getRoles() != null) {
            var roles = new HashSet<>(roleRepository.findAllById(request.getRoles()));
            user.setRoles(roles);
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(String userId) {
        /*
         * Tại sao check tồn tại trước khi delete?
         * → deleteById không throw exception nếu không tìm thấy (Spring Boot 3)
         * → Trả về 404 rõ ràng thay vì 200 OK khi delete user không tồn tại
         * → Idempotent vs explicit error: tùy business requirement,
         *   ở đây mình chọn explicit error
         */
        if (!userRepository.existsById(userId))
            throw new AppException(ErrorCode.USER_NOT_EXISTED);

        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }
}