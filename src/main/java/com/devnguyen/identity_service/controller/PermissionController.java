package com.devnguyen.identity_service.controller;

import com.devnguyen.identity_service.dto.request.PermissionRequest;
import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.dto.response.PermissionResponse;
import com.devnguyen.identity_service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
 * @RestController: nhận request, trả JSON.
 * @RequestMapping("/permissions"): prefix URL cho tất cả endpoint trong class.
 * → Kết hợp với context-path "/identity":
 *   Full URL: /identity/permissions
 *
 * @RequiredArgsConstructor: constructor injection cho PermissionService.
 */
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /*
     * @PostMapping: xử lý HTTP POST → tạo mới resource.
     * @RequestBody: deserialize JSON body → PermissionRequest object.
     *   Jackson đọc Content-Type: application/json → convert JSON thành Java object.
     *
     * Tại sao không có @Valid ở đây?
     * → PermissionRequest không có validation annotation
     * → Nếu có → thêm @Valid vào
     *
     * ApiResponse.<PermissionResponse>builder():
     * → Wrap kết quả vào ApiResponse format chuẩn
     * → Client nhận: { "code": 1000, "result": { "name": "...", ... } }
     */
    @PostMapping
    public ApiResponse<PermissionResponse> create(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.create(request))
                .build();
    }

    /*
     * @GetMapping: xử lý HTTP GET → lấy resource.
     * Không có @PathVariable hay @RequestBody vì lấy tất cả.
     */
    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissionService.getAll())
                .build();
    }

    /*
     * @DeleteMapping("/{name}"): xử lý HTTP DELETE với path variable.
     * @PathVariable String name: extract "name" từ URL path.
     * Ví dụ: DELETE /identity/permissions/CREATE_DATA → name = "CREATE_DATA"
     *
     * Trả về ApiResponse<Void> vì delete không có data trả về.
     * Void: không có result → JSON: { "code": 1000 } (không có "result" field vì NON_NULL)
     */
    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable String name) {
        permissionService.delete(name);
        return ApiResponse.<Void>builder().build();
    }
}
