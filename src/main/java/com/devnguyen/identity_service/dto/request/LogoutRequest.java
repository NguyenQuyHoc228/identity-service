package com.devnguyen.identity_service.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutRequest {
    private String token;
}