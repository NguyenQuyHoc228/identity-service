package com.devnguyen.identity_service.repository;

import com.devnguyen.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/*
 * @Repository: đánh dấu đây là Spring Bean, đồng thời enable exception translation.
 * Exception translation: chuyển các exception JPA cụ thể (HibernateException,
 * PersistenceException...) thành Spring DataAccessException hierarchy.
 * Lợi ích: Service không cần biết bạn đang dùng JPA hay JDBC hay gì khác.
 *
 * JpaRepository<User, String>:
 *   - User   = Entity type
 *   - String = kiểu của Primary Key (id là String/UUID)
 *
 * Spring Data JPA tự generate implementation lúc runtime dựa trên tên method!
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /*
     * Spring Data JPA Query Derivation:
     * Chỉ cần đặt tên method đúng cú pháp → Spring tự generate SQL.
     *
     * findByUsername → SELECT * FROM users WHERE username = ?
     *
     * Tại sao trả Optional<User> thay vì User?
     * - User có thể null nếu không tìm thấy
     * - Optional buộc caller phải xử lý trường hợp null explicitly
     * - Tránh NullPointerException âm thầm
     * - Code rõ ràng hơn: userRepository.findByUsername("john")
     *                       .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
     */
    Optional<User> findByUsername(String username);

    /*
     * existsBy... → Spring generate: SELECT COUNT(*) > 0 FROM users WHERE username = ?
     * Tại sao không dùng findByUsername rồi check null?
     * → existsByUsername chỉ SELECT COUNT, không load toàn bộ object
     * → Hiệu quả hơn khi chỉ cần biết có tồn tại hay không (ví dụ: check duplicate)
     */
    boolean existsByUsername(String username);
}