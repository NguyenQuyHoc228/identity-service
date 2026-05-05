package com.devnguyen.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    private String name;

    private String description;

    /*
     * @ManyToMany: Quan hệ nhiều-nhiều giữa Role và Permission.
     * Hibernate sẽ tạo join table để lưu quan hệ này.
     *
     * @JoinTable: Tùy chỉnh join table thay vì để Hibernate tự đặt tên.
     *   name = "role_permission"         → tên bảng join table
     *   joinColumns                      → FK trỏ về bảng hiện tại (role)
     *   inverseJoinColumns               → FK trỏ về bảng bên kia (permission)
     *
     * Tại sao khai báo tường minh?
     * → Hibernate tự đặt tên thường là "role_permissions" hoặc tuỳ version
     *   → không nhất quán giữa các môi trường
     *   → tường minh giúp DBA biết chính xác cấu trúc DB
     *
     * FetchType.LAZY: chỉ load permissions khi gọi role.getPermissions()
     * → Tránh N+1 query problem và tránh load dữ liệu không cần thiết
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_name"),
            inverseJoinColumns = @JoinColumn(name = "permission_name")
    )
    private Set<Permission> permissions;

    /*
     * Tại sao dùng Set thay vì List?
     * List cho phép duplicate → một Role có thể có cùng Permission 2 lần → vô nghĩa
     * Set đảm bảo unique → đúng với semantic của "tập hợp permission"
     * Ngoài ra, Hibernate có vấn đề với List trong @ManyToMany (Hibernate bug):
     * khi xóa 1 phần tử, nó DELETE ALL rồi INSERT lại → rất tệ về performance
     * Set không có vấn đề này
     */
}