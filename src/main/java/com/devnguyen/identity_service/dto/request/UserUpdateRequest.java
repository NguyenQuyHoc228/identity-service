package com.devnguyen.identity_service.dto.request;

import com.devnguyen.identity_service.validator.DobConstraint;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

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