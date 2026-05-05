package com.devnguyen.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/*
 * Wrapper cho TẤT CẢ response trả về client.
 * Tại sao cần wrapper này?
 *
 * Nếu không có wrapper:
 *   GET /users/123 thành công → trả thẳng UserResponse object
 *   GET /users/999 thất bại   → trả error object (format khác!)
 *
 * Client phải xử lý 2 format khác nhau → phức tạp.
 *
 * Với wrapper:
 *   Mọi response đều có format: { code, message, result }
 *   Thành công: { "code": 1000, "result": {...} }
 *   Thất bại:   { "code": 1001, "message": "User not found" }
 *   → Client chỉ cần check "code" để biết success hay fail
 *
 * Generic type <T>: result có thể là bất kỳ kiểu nào
 *   UserResponse, List<UserResponse>, AuthenticationResponse...
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/*
 * @JsonInclude(JsonInclude.Include.NON_NULL):
 * Không serialize field nếu giá trị là null.
 * Ví dụ: response thành công không có "message" → không include field "message" trong JSON
 * → JSON gọn hơn, client không bị confused bởi null fields
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /*
     * code mặc định là 1000 = SUCCESS.
     * Dùng int thay vì String vì:
     * → Dễ compare: if (response.code == 1000)
     * → Ít tốn bandwidth hơn
     * → Convention trong nhiều hệ thống (HTTP status code cũng là int)
     */
    @Builder.Default
    private int code = 1000;

    private String message;

    /*
     * result: data thực sự trả về.
     * null khi có lỗi (và JsonInclude.NON_NULL sẽ bỏ field này)
     */
    private T result;
}