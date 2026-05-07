package com.devnguyen.identity_service.exception;

import com.devnguyen.identity_service.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /*
     * Attribute key để lấy ConstraintViolation từ MethodArgumentNotValidException.
     * Dùng khi custom validation (@DobConstraint) có attribute động ({min}).
     */
    private static final String MIN_ATTRIBUTE = "min";

    /*
      Handler 1: AppException - lỗi business logic của chúng ta
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                // HTTP status từ ErrorCode (404, 400, 401, 403...)
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /*
      Handler 2: MethodArgumentNotValidException - @Valid fail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(
            MethodArgumentNotValidException e) {

        String enumKey = e.getFieldError().getDefaultMessage();

        // Default nếu enumKey không map được với ErrorCode nào
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolation = e.getBindingResult()
                    .getAllErrors()
                    .getFirst()
                    .unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info("Attributes(Thuộc tính): {}", attributes);

        } catch (IllegalArgumentException ex) {

            log.warn("Mã lỗi không xác định: {}", enumKey);
        }

        String message = Objects.nonNull(attributes)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .build());
    }

    /*
      Handler 3: AccessDeniedException - có authenticate nhưng không có quyền (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /*
      Handler 4: AuthenticationException - chưa authenticate (401)
      Spring Security throw khi token invalid, expired, hoặc không có token.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(
            AuthenticationException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /*
      Handler 5: RuntimeException - lưới an toàn cuối cùng
      Bất kỳ exception nào không được handle bởi các handler trên
      đều rơi vào đây → trả về 500 với message chung chung.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException e) {
        // Log để developer debug, nhưng không trả ra client
        log.error("Ngoại lệ chưa được xử lý: ", e);

        return ResponseEntity
                .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus())
                .body(ApiResponse.builder()
                        .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}