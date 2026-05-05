package com.devnguyen.identity_service.configuration;

import com.devnguyen.identity_service.entity.Role;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.enums.PredefinedRole;
import com.devnguyen.identity_service.repository.RoleRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

/*
 * ApplicationRunner: interface Spring Boot, method run() được gọi
 * SAU KHI application khởi động xong (sau khi tất cả Bean được tạo).
 *
 * Tại sao dùng ApplicationRunner thay vì @PostConstruct?
 * → @PostConstruct chạy khi Bean được tạo — lúc đó Spring context
 *   có thể chưa sẵn sàng hoàn toàn (transaction, security chưa setup)
 * → ApplicationRunner chạy sau khi TOÀN BỘ context ready
 * → An toàn hơn để thực hiện DB operations lúc startup
 *
 * @Configuration: đây là class cấu hình, chứa @Bean methods
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {

    private final PasswordEncoder passwordEncoder;

    /*
     * @Bean ApplicationRunner: đăng ký ApplicationRunner như một Spring Bean.
     *
     * Tại sao inject UserRepository và RoleRepository vào @Bean method
     * thay vì inject vào class?
     * → Tránh circular dependency với SecurityConfig
     *   (SecurityConfig cần PasswordEncoder, ApplicationInitConfig cần PasswordEncoder)
     * → Spring tự inject parameter của @Bean method
     */
    @Bean
    ApplicationRunner applicationRunner(
            UserRepository userRepository,
            RoleRepository roleRepository) {

        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                initRoles(roleRepository);
                initAdminUser(userRepository, roleRepository);
            }
        };
    }

    private void initRoles(RoleRepository roleRepository) {
        /*
         * Tạo role USER nếu chưa tồn tại.
         * existsById(name): PK của Role là name → findById("USER")
         */
        if (!roleRepository.existsById(PredefinedRole.USER_ROLE)) {
            roleRepository.save(Role.builder()
                    .name(PredefinedRole.USER_ROLE)
                    .description("User role - basic access")
                    .build());
            log.info("Created USER role");
        }

        if (!roleRepository.existsById(PredefinedRole.ADMIN_ROLE)) {
            roleRepository.save(Role.builder()
                    .name(PredefinedRole.ADMIN_ROLE)
                    .description("Admin role - full access")
                    .build());
            log.info("Created ADMIN role");
        }
    }

    private void initAdminUser(
            UserRepository userRepository,
            RoleRepository roleRepository) {

        /*
         * Chỉ tạo admin nếu chưa tồn tại.
         * Tại sao check existsByUsername thay vì findByUsername?
         * → existsByUsername: SELECT COUNT(*) → chỉ quan tâm có hay không
         * → findByUsername: SELECT * → load toàn bộ object → tốn hơn
         * → Dùng phương thức phù hợp với mục đích
         */
        if (!userRepository.existsByUsername("admin")) {

            /*
             * FIX BUG project gốc: roles bị comment out → admin không có role
             * → Sửa: fetch ADMIN role từ DB và gán cho admin user
             */
            var adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            var roles = new HashSet<Role>();
            roles.add(adminRole);

            User admin = User.builder()
                    .username("admin")
                    /*
                     * Encode password "admin" bằng BCrypt.
                     * Trong production: đọc từ biến môi trường
                     * .password(passwordEncoder.encode(System.getenv("ADMIN_PASSWORD")))
                     */
                    .password(passwordEncoder.encode("admin"))
                    .roles(roles)
                    .build();

            userRepository.save(admin);
            log.warn("Admin user created with default password. " +
                    "Please change it immediately in production!");
        }
    }
}