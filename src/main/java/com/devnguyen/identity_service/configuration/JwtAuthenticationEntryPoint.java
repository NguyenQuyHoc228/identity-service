package com.devnguyen.identity_service.configuration;

import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * AuthenticationEntryPoint: được gọi khi request CHƯA authenticate
 * nhưng cố gắng truy cập protected resource.
 *
 * Ví dụ: không có Authorization header, token invalid, token expired
 * → BearerTokenAuthenticationFilter throw AuthenticationException
 * → Spring Security gọi commence() ở đây thay vì forward đến Controller
 * → GlobalExceptionHandler KHÔNG được gọi (vì chưa vào Controller)
 * → Cần tự write response ở đây
 *
 * @Component: đăng ký như Spring Bean để inject vào SecurityConfig
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        /*
         * Set HTTP status: 401 Unauthorized
         * Content-Type: application/json
         * → Client biết đây là JSON response, không phải HTML
         */
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        /*
         * Build ApiResponse theo format chuẩn của mình.
         * Giống hệt format trả về từ GlobalExceptionHandler
         * → Client xử lý nhất quán, không biết lỗi đến từ đâu
         */
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        /*
         * Tại sao không dùng @ResponseBody hay ResponseEntity?
         * → Đây là Servlet-level code, không phải Spring MVC level
         * → Phải tự write vào HttpServletResponse manually
         */
    }
}