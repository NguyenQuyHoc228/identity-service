package com.devnguyen.identity_service.dto.request;

import com.devnguyen.identity_service.validator.DobConstraint;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

/*
 * Tại sao không @Entity ở đây? DTO là plain Java object,
 * không liên quan đến DB, không có @Id, không có relationship.
 * Nó chỉ là "data container" để nhận data từ HTTP request body.
 */
@Data  // @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
// Dùng @Data cho DTO vì DTO cần cả getter lẫn setter (Jackson dùng setter để deserialize)
// Không dùng @Data cho Entity vì @EqualsAndHashCode trên Entity có thể gây vấn đề với JPA
public class UserCreationRequest {

    /*
     * @Size(min=3): username phải có ít nhất 3 ký tự.
     * message: thông báo lỗi custom thay vì default của Spring.
     *
     * Tại sao validate ở DTO thay vì Service?
     * → DTO là "cổng vào" của request. Validate sớm nhất có thể.
     * → Service không cần lo về format của input, chỉ lo business logic.
     * → Nếu validate trong Service, bạn phải viết if-else thủ công.
     */
    @Size(min = 3, message = "USERNAME_INVALID")
    private String username;

    /*
     * Tại sao message là error code ("PASSWORD_INVALID") thay vì text ("Password too short")?
     * → Sau này dùng error code để map với ErrorCode enum → trả về structured error response
     * → Dễ i18n (internationalization) hơn nếu cần đa ngôn ngữ
     */
    @Size(min = 8, message = "PASSWORD_INVALID")
    private String password;

    private String firstName;
    private String lastName;

    /*
     * @DobConstraint: custom validation annotation mình sẽ tạo ở Giai đoạn 4.
     * min = 18: phải từ 18 tuổi trở lên.
     * Tại sao cần custom annotation?
     * → @Past chỉ check ngày trong quá khứ, không check khoảng cách.
     * → Không có annotation built-in nào check "tuổi tối thiểu".
     */
    @DobConstraint(min = 18, message = "INVALID_DOB")
    private LocalDate dob;

    /*
     * roles: client có thể gửi danh sách role khi tạo user.
     * Set<String> thay vì Set<Role> vì client chỉ gửi tên role (String),
     * không gửi cả object Role. Service sẽ lookup Role từ DB.
     */
    private Set<String> roles;
}