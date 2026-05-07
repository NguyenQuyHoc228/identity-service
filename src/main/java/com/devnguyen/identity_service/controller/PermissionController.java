package com.devnguyen.identity_service.controller;

import com.devnguyen.identity_service.dto.request.PermissionRequest;
import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.dto.response.PermissionResponse;
import com.devnguyen.identity_service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ApiResponse<PermissionResponse> create(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable String name) {
        permissionService.delete(name);
        return ApiResponse.<Void>builder().build();
    }
}
