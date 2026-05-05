package com.devnguyen.identity_service.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.util.Set;

/*
 * Tại sao dùng @Builder thay vì @Data cho Response DTO?
 *
 * Response DTO được tạo bởi Mapper (hoặc Service), trả ra client.
 * Sau khi tạo ra, không ai nên thay đổi nó nữa → ideally immutable.
 * @Builder + @Getter (không có @Setter) = immutable object(đối tượng bất biến).
 *
 * Thực tế: MapStruct cần setter hoặc builder để set field.
 * Nếu dùng @Builder, cần config MapStruct dùng builder.
 * Để đơn giản, project này dùng @Data (có setter) cho cả Response DTO.
 * Đây là trade-off (sự đánh đổi) giữa purity và simplicity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String username;

    // Không có password! Đây là lý do cần DTO riêng.

    private String firstName;
    private String lastName;
    private LocalDate dob;

    /*
     * Trả về Set<RoleResponse> thay vì Set<Role> (entity).
     * → Tránh circular reference: User → Role → Permission → ...
     * → RoleResponse chỉ chứa data cần thiết cho client
     */
    private Set<RoleResponse> roles;
}