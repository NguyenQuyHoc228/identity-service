package com.devnguyen.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "users")  // "user" là từ khóa dành riêng trong MySQL → dùng "users"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /*
     * UUID làm Primary Key.
     * @GeneratedValue(strategy = GenerationType.UUID):
     *   Spring Boot 3 / Hibernate 6 hỗ trợ native UUID generation
     *   Hibernate tự generate UUID trước khi INSERT → không cần round-trip DB
     *
     * Tại sao String thay vì java.util.UUID?
     *   String đơn giản hơn, JSON serialize thành chuỗi tự nhiên,
     *   không cần custom converter
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /*
     * @Column(unique = true, nullable = false):
     *   unique = true   → tạo UNIQUE constraint trên bảng → DB đảm bảo không trùng
     *   nullable = false → NOT NULL constraint(ràng buộc)
     *
     *   Tại sao cần constraint ở DB level nếu đã validate ở application level?
     *   → Có thể có nhiều instance của service chạy song song (horizontal scaling)
     *   → Race condition: 2 request đến cùng lúc, cả 2 đều pass validation,
     *     nhưng chỉ 1 được INSERT → DB constraint là lưới an toàn cuối cùng
     */
    @Column(unique = true, nullable = false)
    private String username;

    /*
     * Không @Column(nullable = false) cho password?
     * Vì sau này có thể có OAuth2 login (Google, Facebook) → không có password
     * Linh hoạt hơn cho tương lai
     */
    private String password;

    private String firstName;
    private String lastName;

    /*
     * LocalDate thay vì Date hay LocalDateTime vì:
     *   - Date of birth chỉ cần ngày, không cần giờ
     *   - LocalDate là immutable, thread-safe
     *   - java.util.Date đã deprecated từ Java 8
     *   - LocalDate không có timezone issue (Date/LocalDateTime có)
     */
    private LocalDate dob;

    /*
     * FIX BUG project gốc: Bỏ @JdbcTypeCode(SqlTypes.JSON)
     * Chỉ dùng @ManyToMany thuần → Hibernate tạo join table "user_role"
     *
     * mappedBy = "users" nếu là bidirectional, nhưng ở đây ta dùng unidirectional:
     * User biết về Role, Role không cần biết về User
     * → Đơn giản hơn, ít code hơn, tránh circular reference khi serialize JSON
     *
     * CascadeType: KHÔNG set cascade ở đây.
     * Tại sao? Nếu set CascadeType.ALL: xóa User → xóa luôn Role → sai hoàn toàn!
     * Role là shared entity, nhiều User dùng chung → không cascade
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    private Set<Role> roles;
}