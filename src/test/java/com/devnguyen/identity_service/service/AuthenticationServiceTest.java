package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.AuthenticationRequest;
import com.devnguyen.identity_service.dto.request.IntrospectRequest;
import com.devnguyen.identity_service.dto.request.LogoutRequest;
import com.devnguyen.identity_service.dto.response.AuthenticationResponse;
import com.devnguyen.identity_service.entity.InvalidatedToken;
import com.devnguyen.identity_service.entity.Role;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.repository.InvalidatedTokenRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        /*
         * ReflectionTestUtils.setField(): inject giá trị vào field @Value.
         *
         * Vấn đề: @Value("${jwt.signerKey}") được inject bởi Spring context.
         * Trong Unit Test không có Spring context → field sẽ là null.
         *
         * Giải pháp: ReflectionTestUtils.setField() dùng reflection để
         * set giá trị trực tiếp vào field private, bypass Spring injection.
         *
         * Tại sao cần key đủ dài?
         * HS512 yêu cầu key ≥ 512 bits = 64 bytes
         * String 64 ký tự = 64 bytes ✓
         */
        ReflectionTestUtils.setField(
                authenticationService,
                "signerKey",
                "29da87f839479eab94bb191c6f21f37127a5b6ba3c4a6c01593d91f05905019f"
        );
        ReflectionTestUtils.setField(
                authenticationService,
                "validDuration",
                3600L
        );
        ReflectionTestUtils.setField(
                authenticationService,
                "refreshableDuration",
                36000L
        );

        mockUser = User.builder()
                .id("test-uuid-123")
                .username("john")
                .password("encoded_password")
                .roles(Set.of(Role.builder()
                        .name("USER")
                        .permissions(Set.of())
                        .build()))
                .build();
    }

    // ==================== authenticate Tests ====================

    @Test
    void authenticate_validCredentials_returnsTokenResponse() {
        // ARRANGE
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("john");
        request.setPassword("password123");

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        /*
         * passwordEncoder.matches(raw, encoded):
         * Mock trả về true = password đúng
         */
        when(passwordEncoder.matches("password123", "encoded_password"))
                .thenReturn(true);

        // ACT
        AuthenticationResponse response = authenticationService.authenticate(request);

        // ASSERT
        assertThat(response.isAuthenticated()).isTrue();
        /*
         * Token không phải null và không rỗng.
         * Không test exact value của token vì token chứa timestamp
         * → mỗi lần generate sẽ khác nhau
         * → chỉ cần đảm bảo token được tạo ra (not null, not empty)
         */
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotEmpty();
    }

    @Test
    void authenticate_userNotFound_throwsException() {
        // ARRANGE
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("nobody");
        request.setPassword("password123");

        when(userRepository.findByUsername("nobody"))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.authenticate(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED);
        // Không check password khi user không tồn tại
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticate_wrongPassword_throwsUnauthenticatedException() {
        // ARRANGE
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("john");
        request.setPassword("wrong_password");

        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        // Mock: password sai → trả về false
        when(passwordEncoder.matches("wrong_password", "encoded_password"))
                .thenReturn(false);

        // ACT & ASSERT
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.authenticate(request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    // ==================== introspect Tests ====================

    @Test
    void introspect_validToken_returnsAuthenticated() throws Exception {
        // ARRANGE: Tạo token thật bằng cách login trước
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setUsername("john");
        loginRequest.setPassword("password123");
        String token = authenticationService.authenticate(loginRequest).getToken();

        // Mock: token chưa bị blacklist
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        // ACT
        AuthenticationResponse response = authenticationService.introspect(
                IntrospectRequest.builder().token(token).build()
        );

        // ASSERT
        assertThat(response.isAuthenticated()).isTrue();
    }

    @Test
    void introspect_invalidToken_returnsNotAuthenticated() throws Exception {
        // ARRANGE: token rác không hợp lệ
        IntrospectRequest request = IntrospectRequest.builder()
                .token("this.is.invalid.token")
                .build();

        // ACT
        AuthenticationResponse response = authenticationService.introspect(request);

        // ASSERT
        assertThat(response.isAuthenticated()).isFalse();
    }

    @Test
    void introspect_blacklistedToken_returnsNotAuthenticated() throws Exception {
        // ARRANGE: Tạo token hợp lệ trước
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        String token = authenticationService.authenticate(
                new AuthenticationRequest() {{ setUsername("john"); setPassword("pass"); }}
        ).getToken();

        // Mock: token đã bị blacklist (đã logout)
        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(true);

        // ACT
        AuthenticationResponse response = authenticationService.introspect(
                IntrospectRequest.builder().token(token).build()
        );

        // ASSERT: token trong blacklist → không hợp lệ
        assertThat(response.isAuthenticated()).isFalse();
    }

    // ==================== logout Tests ====================

    @Test
    void logout_validToken_savesTokenToBlacklist() throws Exception {
        // ARRANGE: Tạo token hợp lệ
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        String token = authenticationService.authenticate(
                new AuthenticationRequest() {{ setUsername("john"); setPassword("pass"); }}
        ).getToken();

        when(invalidatedTokenRepository.existsById(anyString())).thenReturn(false);

        // ACT
        authenticationService.logout(LogoutRequest.builder().token(token).build());

        // ASSERT
        /*
         * Verify invalidatedTokenRepository.save() được gọi đúng 1 lần
         * → Đảm bảo jti được lưu vào blacklist khi logout
         */
        verify(invalidatedTokenRepository, times(1))
                .save(any(InvalidatedToken.class));
    }
}