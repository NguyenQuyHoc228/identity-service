package com.devnguyen.identity_service.dto.request;

import lombok.Data;

@Data
public class RefreshRequest {
    private String token;
}