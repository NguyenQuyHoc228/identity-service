package com.devnguyen.identity_service.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class RoleRequest {
    private String name;
    private String description;

    /*
     * permissions: danh sách tên permission gán cho role này.
     * Set<String> vì client chỉ gửi tên, không gửi cả object.
     */
    private Set<String> permissions;
}