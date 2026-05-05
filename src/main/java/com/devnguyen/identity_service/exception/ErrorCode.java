package com.devnguyen.identity_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/*
 * Enum chứa tất cả error code của hệ thống.
 *
 * Tại sao dùng enum thay vì constants class?
 * → Enum là type-safe: không thể truyền nhầm giá trị
 * → Enum có thể có method, field → mỗi code mang thêm metadata (httpStatus, message)
 * → Dễ iterate, dễ document
 *
 * Convention đặt code:
 *   1xxx = lỗi chung / validation
 *   2xxx = lỗi authentication
 *   3xxx = lỗi authorization
 *   9xxx = lỗi hệ thống
 * → Nhìn code biết ngay thuộc nhóm nào
 *
 * FIX BUG project gốc: DOB_INVALID và UN_AUTHORIZED đều là 1007
 * → Sửa thành code riêng biệt
 */
@Getter
public enum ErrorCode {

    // ===== 1xxx: Lỗi chung & validation =====
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không được phân loại", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Khóa tin nhắn không hợp lệ", HttpStatus.BAD_REQUEST),

    /*
     * USER_EXISTED vs USER_NOT_EXISTED: 2 trường hợp khác nhau hoàn toàn
     * → code khác nhau để client xử lý khác nhau
     * (ví dụ: USER_EXISTED → hiển thị "Username đã tồn tại, thử username khác"
     *         USER_NOT_EXISTED → redirect về login page)
     */
    USER_EXISTED(1002, "Người dùng đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Tên người dùng phải có ít nhất 3 ký tự.", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Mật khẩu phải có ít nhất 8 ký tự.", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),

    /*
     * INVALID_DOB: FIX - đổi code từ 1007 (trùng) sang 1006
     * HttpStatus.BAD_REQUEST vì đây là lỗi input của client
     */
    INVALID_DOB(1006, "Tuổi của bạn phải ít nhất {min}", HttpStatus.BAD_REQUEST),

    // ===== 2xxx: Lỗi Authentication =====
    /*
     * UNAUTHENTICATED vs UNAUTHORIZED: 2 khái niệm KHÁC NHAU, hay bị nhầm!
     *
     * UNAUTHENTICATED (401): "Tôi không biết bạn là ai"
     *   → Chưa login, token không hợp lệ, token hết hạn
     *
     * UNAUTHORIZED (403): "Tôi biết bạn là ai, nhưng bạn không có quyền làm việc này"
     *   → Đã login, nhưng role/permission không đủ
     *
     * Rất nhiều developer dùng sai 401/403!
     */
    UNAUTHENTICATED(2001, "Chưa được xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(3001, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    ;

    /*
     * Tại sao lưu cả code (int) lẫn httpStatus (HttpStatus)?
     * → httpStatus: dùng để set HTTP status code trong response header
     *   (Spring sẽ trả về HTTP 404, 401, 403... đúng chuẩn REST)
     * → code (int): dùng trong response body để client xử lý chi tiết hơn
     *   HTTP 400 có thể là nhiều loại lỗi khác nhau (1002, 1003, 1004...)
     *   → client dùng code trong body để hiển thị đúng message
     */
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}