package com.devnguyen.identity_service.controller;

import com.devnguyen.identity_service.dto.request.UserCreationRequest;
import com.devnguyen.identity_service.dto.request.UserUpdateRequest;
import com.devnguyen.identity_service.dto.response.ApiResponse;
import com.devnguyen.identity_service.dto.response.UserResponse;
import com.devnguyen.identity_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/*
 * FIX BUG project gốc: dùng @RequiredArgsConstructor (constructor injection)
 * thay vì @Autowired field injection cho nhất quán và testable hơn.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /*
     * @Valid: kích hoạt Bean Validation cho UserCreationRequest.
     * → Spring check tất cả annotation (@Size, @DobConstraint...) trên request
     * → Nếu fail → throw MethodArgumentNotValidException
     * → GlobalExceptionHandler bắt và trả về error response chuẩn
     *
     * @Valid đặt trước @RequestBody: validate TRƯỚC khi method chạy.
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    /*
     * Endpoint này yêu cầu ROLE_ADMIN (check trong Service bằng @PreAuthorize).
     * Controller không biết/không quan tâm về authorization logic.
     * → Separation of concerns: Controller lo HTTP, Service lo business + security.
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    /*
     * "/myInfo": đặt TRƯỚC "/{id}" vì Spring match URL theo thứ tự.
     * Nếu "/{id}" đặt trước → "/myInfo" bị match như id="myInfo" → logic sai.
     *
     * Thực tế trong Spring Boot: @GetMapping("/myInfo") và @GetMapping("/{id}")
     * → Spring ưu tiên literal path ("/myInfo") hơn path variable ("/{id}")
     * → Thứ tự khai báo không quan trọng, nhưng để trên cho rõ ràng.
     */
    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(id))
                .build();
    }

    /*
     * @PutMapping("/{id}"): HTTP PUT → update toàn bộ resource.
     * (Strict REST: PUT = replace toàn bộ, PATCH = update một phần)
     * Project này dùng PUT nhưng implement như PATCH (nhờ IGNORE_NULL trong mapper)
     * → Đây là common trade-off trong thực tế
     */
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder().build();
    }
}