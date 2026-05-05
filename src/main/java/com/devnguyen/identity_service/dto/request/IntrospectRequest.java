package com.devnguyen.identity_service.dto.request;

import lombok.Data;
import lombok.Builder;          // ← Thêm cái này
import lombok.NoArgsConstructor; // ← Thêm cái này
import lombok.AllArgsConstructor; // ← Thêm cái này

@Data
@Builder          // ← Đây là nguyên nhân lỗi, thiếu annotation này
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectRequest {
    private String token;
}