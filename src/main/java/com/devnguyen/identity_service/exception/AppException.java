package com.devnguyen.identity_service.exception;

import lombok.Getter;

/*
 * Custom RuntimeException của hệ thống.
 *
 * Tại sao extend RuntimeException thay vì Exception (checked)?
 *
 * Checked Exception (extend Exception):
 *   → Bắt buộc caller phải try-catch hoặc throws
 *   → Ví dụ: IOException, SQLException
 *   → Phù hợp khi caller CÓ THỂ recover (đọc file lỗi → thử file khác)
 *
 * Unchecked Exception (extend RuntimeException):
 *   → Không bắt buộc handle
 *   → Phù hợp khi lỗi là do programming error hoặc business rule violation
 *     mà caller không thể recover (user not found → không làm gì được, phải báo lỗi)
 *   → Spring @ExceptionHandler sẽ bắt thay cho mình
 *   → Code sạch hơn vì không phải throws ở mọi method
 *
 * Trong thực tế đi làm, business exception hầu như luôn là RuntimeException.
 */
@Getter
public class AppException extends RuntimeException {

    /*
     * Lưu ErrorCode thay vì String message.
     * Tại sao? ErrorCode chứa đầy đủ thông tin: code, message, httpStatus.
     * GlobalExceptionHandler sẽ dùng errorCode để build response.
     */
    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        /*
         * Truyền message lên RuntimeException để có thể log được.
         * Khi log exception: logger.error("Error", e) → in ra errorCode.getMessage()
         */
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}