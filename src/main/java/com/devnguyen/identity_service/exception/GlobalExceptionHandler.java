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

/*
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * → Bắt exception từ tất cả @RestController
 * → Tự động serialize return value thành JSON (nhờ @ResponseBody)
 *
 * @Slf4j: Lombok inject Logger.
 * Tại sao cần log?
 * → Debug production issues khi không có debugger
 * → Audit trail: biết ai gọi gì, lỗi gì xảy ra lúc mấy giờ
 * → Monitor: đếm lỗi để cảnh báo khi hệ thống có vấn đề
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /*
     * Attribute key để lấy ConstraintViolation từ MethodArgumentNotValidException.
     * Dùng khi custom validation (@DobConstraint) có attribute động ({min}).
     */
    private static final String MIN_ATTRIBUTE = "min";

    /*
     * Handler 1: AppException - lỗi business logic của chúng ta
     *
     * @ExceptionHandler(AppException.class):
     * Spring gọi method này khi bất kỳ Controller nào throw AppException.
     *
     * ResponseEntity<ApiResponse>:
     * → ResponseEntity: cho phép control cả HTTP status code lẫn body
     * → ApiResponse: body theo format chuẩn của mình
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
     * Handler 2: MethodArgumentNotValidException - @Valid fail
     *
     * Khi @Valid fail, Spring throw MethodArgumentNotValidException.
     * Exception này chứa danh sách tất cả field bị lỗi.
     *
     * Chiến lược ở đây: lấy lỗi ĐẦU TIÊN, map sang ErrorCode, trả về.
     * (Có thể trả về tất cả lỗi cùng lúc, nhưng phức tạp hơn - đủ dùng với 1 lỗi)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(
            MethodArgumentNotValidException e) {

        /*
         * getFieldError().getDefaultMessage() trả về message trong annotation.
         * Ví dụ: @Size(min=3, message="USERNAME_INVALID") → "USERNAME_INVALID"
         * Mình dùng message như error code key để lookup trong ErrorCode enum.
         */
        String enumKey = e.getFieldError().getDefaultMessage();

        // Default nếu enumKey không map được với ErrorCode nào
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            /*
             * Lấy ConstraintViolation để đọc attribute của annotation.
             * Ví dụ: @DobConstraint(min=18) → attributes = {"min": 18}
             * Dùng để replace placeholder {min} trong message.
             */
            var constraintViolation = e.getBindingResult()
                    .getAllErrors()
                    .getFirst()
                    .unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info("Attributes(Thuộc tính): {}", attributes);

        } catch (IllegalArgumentException ex) {
            /*
             * Nếu enumKey không có trong ErrorCode enum → IllegalArgumentException
             * → giữ nguyên errorCode = ErrorCode.INVALID_KEY
             * → log để developer biết cần thêm ErrorCode mới
             */
            log.warn("Mã lỗi không xác định: {}", enumKey);
        }

        /*
         * Nếu message có placeholder {min} → replace với giá trị thực.
         * Ví dụ: "Your age must be at least {min}" + attributes{"min":18}
         *      → "Your age must be at least 18"
         */
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
     * Handler 3: AccessDeniedException - có authenticate nhưng không có quyền (403)
     *
     * Spring Security throw cái này khi @PreAuthorize fail.
     * Tại sao handle riêng thay vì để Handler 4 (RuntimeException) bắt?
     * → Phân biệt rõ 403 vs 500: AccessDenied là business case bình thường,
     *   không phải lỗi hệ thống → không nên log như error, trả về 403 đúng chuẩn
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
     * Handler 4: AuthenticationException - chưa authenticate (401)
     *
     * Spring Security throw khi token invalid, expired, hoặc không có token.
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
     * Handler 5: RuntimeException - lưới an toàn cuối cùng
     *
     * Bất kỳ exception nào không được handle bởi các handler trên
     * đều rơi vào đây → trả về 500 với message chung chung.
     *
     * Tại sao không expose chi tiết lỗi?
     * → Stack trace có thể chứa thông tin nhạy cảm (tên class, cấu trúc code...)
     * → Attacker có thể dùng thông tin này để tìm vulnerability
     * → Log chi tiết phía server, trả về message chung cho client
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

    /*
     * Helper: replace placeholder trong message với giá trị thực từ attributes.
     * "{min}" → "18" (lấy từ annotation attribute)
     */
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}