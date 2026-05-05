package com.devnguyen.identity_service.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    /*
     * token: JWT string trả về sau khi login thành công.
     * Client lưu token này (localStorage, cookie, memory...)
     * và gửi kèm trong header Authorization: Bearer <token> cho mỗi request tiếp theo.
     */
    private String token;

    /*
     * authenticated: flag cho biết login có thành công không.
     * Tại sao cần field này nếu đã trả token?
     * → Khi introspect: không có token mới, chỉ cần biết valid/invalid
     * → Consistent response format cho cả login và introspect endpoint
     */
    private boolean authenticated;
}