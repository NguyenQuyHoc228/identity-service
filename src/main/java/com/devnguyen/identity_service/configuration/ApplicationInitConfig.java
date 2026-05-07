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

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {

    private final PasswordEncoder passwordEncoder;

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

        if (!userRepository.existsByUsername("admin")) {

            var adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            var roles = new HashSet<Role>();
            roles.add(adminRole);

            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .roles(roles)
                    .build();

            userRepository.save(admin);
            log.warn("Admin user created with default password. " +
                    "Please change it immediately in production!");
        }
    }
}