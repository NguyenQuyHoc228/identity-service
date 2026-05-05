package com.devnguyen.identity_service.controller;

import com.devnguyen.identity_service.dto.request.*;
import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.dto.response.AuthenticationResponse;
import com.devnguyen.identity_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

/*
 * Tất cả endpoint trong controller này là PUBLIC (không cần authenticate).
 * Lý do: đây là endpoint để authenticate → chưa có token thì không thể yêu cầu token.
 * → SecurityConfig sẽ permit tất cả request đến "/auth/**"
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /*
     * POST /identity/auth/token
     * → Đăng nhập, nhận JWT token
     * → Public endpoint
     */
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /*
     * POST /identity/auth/introspect
     * → Kiểm tra token có hợp lệ không
     * → Public: client (hoặc service khác) gọi để verify token
     *
     * throws JOSEException, ParseException:
     * → Các exception của Nimbus library khi parse/verify JWT
     * → Nếu throw → GlobalExceptionHandler catch RuntimeException → 500
     * → Cải tiến: wrap trong try-catch và throw AppException cụ thể hơn
     */
    @PostMapping("/introspect")
    public ApiResponse<AuthenticationResponse> introspect(
            @RequestBody IntrospectRequest request)
            throws JOSEException, ParseException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /*
     * POST /identity/auth/logout
     * → Blacklist token hiện tại
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request)
            throws JOSEException, ParseException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    /*
     * POST /identity/auth/refresh
     * → Đổi token cũ lấy token mới
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refreshToken(
            @RequestBody RefreshRequest request)
            throws JOSEException, ParseException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }
}