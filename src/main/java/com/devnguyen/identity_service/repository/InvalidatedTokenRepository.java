package com.devnguyen.identity_service.repository;

import com.devnguyen.identity_service.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    /*
     * Spring Data tự generate:
     * DELETE FROM invalidated_token WHERE expiry_time < ?
     * Xóa tất cả token đã hết hạn trước thời điểm truyền vào
     */
    void deleteByExpiryTimeBefore(Date date);

    // Thêm dòng này để đếm trước khi xóa
    long countByExpiryTimeBefore(Date date);
}