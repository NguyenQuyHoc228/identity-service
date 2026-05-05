package com.devnguyen.identity_service.dto.request;

import com.devnguyen.identity_service.validator.DobConstraint;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

/*
 * Tại sao cần UserUpdateRequest riêng thay vì dùng lại UserCreationRequest?
 *
 * Khi UPDATE, không nhất thiết phải gửi tất cả field.
 * Ví dụ: user chỉ muốn đổi firstName → không cần gửi password.
 *
 * Nếu dùng chung:
 *   - @Size(min=8) trên password → bắt buộc gửi password → không hợp lý khi chỉ đổi tên
 *   - username không được đổi (unique identifier) → không nên có trong update request
 *
 * Ngoài ra, CreateRequest và UpdateRequest thường có validation rule khác nhau.
 */
@Data
public class UserUpdateRequest {

    @Size(min = 8, message = "PASSWORD_INVALID")
    private String password;

    private String firstName;
    private String lastName;

    @DobConstraint(min = 18, message = "INVALID_DOB")
    private LocalDate dob;

    private Set<String> roles;
}