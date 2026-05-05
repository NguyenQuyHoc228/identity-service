package com.devnguyen.identity_service.dto.request;

import lombok.Data;

@Data
public class PermissionRequest {
    private String name;
    private String description;
}