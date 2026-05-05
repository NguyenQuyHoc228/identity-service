package com.devnguyen.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;

/*
 * @Entity: Đánh dấu class này là JPA Entity → Hibernate sẽ map nó với bảng DB
 * @Table: Chỉ định tên bảng. Nếu không có @Table, Hibernate tự đặt tên
 *         theo tên class (Permission → permission hoặc Permission tùy dialect)
 *         → Nên khai báo tường minh để tránh surprise
 */
@Entity
@Table(name = "permission")
/*
 * Lombok annotations:
 * @Getter/@Setter: sinh getter và setter cho tất cả field
 * @NoArgsConstructor: JPA bắt buộc cần constructor không tham số
 *                     (Hibernate dùng reflection để tạo object khi đọc từ DB)
 * @AllArgsConstructor: tiện khi tạo object trong code
 * @Builder: cho phép dùng builder pattern: Permission.builder().name("...").build()
 *           Tại sao dùng Builder? Khi class có nhiều field, constructor dài rất khó đọc:
 *           new Permission("CREATE_DATA", "Create data permission")
 *           → không biết tham số nào là gì nếu không nhìn vào constructor
 *           Builder giải quyết: Permission.builder().name("CREATE_DATA").description("...").build()
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    /*
     * @Id: đây là Primary Key
     * Tại sao không có @GeneratedValue? Vì chúng ta muốn tự quản lý giá trị PK.
     * name là String ("CREATE_DATA", "READ_DATA"...) → không auto-generate được
     * và cũng không muốn auto-generate (lý do đã giải thích ở 2.5)
     */
    @Id
    private String name;

    private String description;
}