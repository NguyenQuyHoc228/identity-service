package com.devnguyen.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

/*
 * Bảng này dùng để blacklist JWT token đã logout.
 *
 * Tại sao cần blacklist?
 * JWT là stateless: server không lưu session, chỉ verify(xác minh) chữ ký.
 * Khi user logout, token vẫn còn hợp lệ đến khi hết hạn.
 * Nếu attacker lấy được token trước khi logout → vẫn dùng được.
 * Solution: lưu jti (JWT ID) của token đã logout → check khi verify.
 *
 * Tại sao lưu jti thay vì toàn bộ token?
 * Token có thể rất dài (header.payload.signature).
 * jti là UUID ngắn gọn, đủ để định danh duy nhất một token.
 */
@Entity
@Table(name = "invalidated_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidatedToken {

    /*
     * jti = JWT ID, được tạo khi generate token (UUID).
     * Dùng làm PK luôn vì nó đã là unique identifier(mã định danh duy nhất) của token.
     * Không cần auto-generate vì giá trị đến từ JWT.
     */
    @Id
    private String id; // chính là jti của JWT

    /*
     * Lưu expiryTime để có thể cleanup.
     * Cron job định kỳ xóa các token đã hết hạn trong bảng này
     * → tránh bảng phình to vô tận
     * (project hiện tại chưa implement cleanup, nhưng thiết kế cho phép)
     */
    private Date expiryTime;
}