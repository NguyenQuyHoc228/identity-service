package com.devnguyen.identity_service.dto.request;

import lombok.Data;

/*
 * Login request: chỉ cần username và password.
 * Không validation annotation ở đây vì:
 * → AuthenticationService sẽ check và throw lỗi với message cụ thể hơn
 *   ("USER_NOT_EXISTED" vs "INVALID_CREDENTIALS")
 * → Nếu dùng @NotBlank thì chỉ biết "field trống", không biết lý do business
 */
@Data
public class AuthenticationRequest {
    private String username;
    private String password;
}