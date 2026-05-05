package com.devnguyen.identity_service.configuration;

import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * AccessDeniedHandler: được gọi khi user ĐÃ authenticate
 * nhưng không có quyền truy cập resource.
 *
 * Ví dụ: user có ROLE_USER cố gắng gọi endpoint cần ROLE_ADMIN
 * → AuthorizationFilter throw AccessDeniedException
 * → Spring Security gọi handle() ở đây
 *
 * Phân biệt với AuthenticationEntryPoint:
 * AuthenticationEntryPoint → 401 (chưa login)
 * AccessDeniedHandler     → 403 (đã login nhưng không có quyền)
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getHttpStatus().value()); // 403
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}