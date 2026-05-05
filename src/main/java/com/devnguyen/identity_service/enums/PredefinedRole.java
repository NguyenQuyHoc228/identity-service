package com.devnguyen.identity_service.enums;

/*
 * Tại sao cần enum này nếu đã có Role entity trong DB?
 *
 * Role entity trong DB có thể thay đổi theo runtime (admin tạo role mới).
 * Nhưng một số role cốt lõi (ADMIN, USER) cần được code biết đến
 * để dùng trong ApplicationInitConfig và các chỗ hardcode logic.
 *
 * Dùng constant(không thay đổi) String thay vì enum vì:
 *   Role name trong DB là String → dùng String constant nhất quán hơn
 *   Không cần .name() hay .toString() mỗi lần dùng
 *
 * Tại sao đặt tên là PredefinedRole thay vì RoleEnum?
 *   "Enum" trong tên class là redundant (class đã là enum rồi)
 *   "Predefined" mô tả đúng ý nghĩa: đây là các role được định nghĩa sẵn
 */
public class PredefinedRole {
    public static final String USER_ROLE = "USER";
    public static final String ADMIN_ROLE = "ADMIN";

    // Private constructor: prevent instantiation(ngăn chặn việc khởi tạo)
    // Đây là utility class(lớp tiện ích), không cần tạo instance(ví dụ)
    private PredefinedRole() {}
}