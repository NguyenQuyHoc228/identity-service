package com.devnguyen.identity_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
 * Tách PasswordEncoder ra class riêng để phá vòng circular dependency.
 *
 * Vấn đề khi để trong SecurityConfig:
 * SecurityConfig → CustomJwtDecoder → AuthenticationService → passwordEncoder
 *                                                                    ↑
 *                                                            nằm trong SecurityConfig
 *                                                            → SecurityConfig chưa xong
 *                                                            → DEADLOCK
 *
 * Giải pháp: PasswordEncoderConfig không phụ thuộc vào ai
 * → Spring tạo nó trước tiên → AuthenticationService inject được
 * → Không còn circular dependency
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}