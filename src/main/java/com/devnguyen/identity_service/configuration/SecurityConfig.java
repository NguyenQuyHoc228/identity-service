package com.devnguyen.identity_service.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/*
  @Configuration: class chứa @Bean definitions

  @EnableWebSecurity: bật Spring Security cho web layer.
  Thực ra Spring Boot auto-config đã bật rồi, nhưng khai báo tường minh
  → Ý định rõ ràng, dễ đọc code

  @EnableMethodSecurity: bật method-level security.
  → Cho phép dùng @PreAuthorize, @PostAuthorize, @Secured trên method
  → Không có annotation này → @PreAuthorize bị ignore hoàn toàn!
  → prePostEnabled = true (default): bật @PreAuthorize và @PostAuthorize
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /*
      PUBLIC_ENDPOINTS: danh sách endpoint không cần authenticate.
      Khai báo là constant để dễ maintain, không hardcode trong config method.
     */
    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/users",
            "/auth/token",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh"
    };

    /*
     Inject CustomJwtDecoder để dùng trong oauth2ResourceServer config.
     */
    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /*
     * SecurityFilterChain Bean: cấu hình toàn bộ Security.
     * HttpSecurity: builder để config Security Filter Chain.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()

                        .anyRequest().authenticated()
                )

                /*
                 2. OAUTH2 RESOURCE SERVER: cấu hình JWT processing
                 */
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder)

                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )

                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                /*
                 * 3. EXCEPTION HANDLING
                 */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                /*
                 4. SESSION MANAGEMENT: STATELESS
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /*
                 5. CSRF: Disable
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                  6. CORS: Enable với default config
                 */
                .cors(cors -> cors.configure(http));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthorityPrefix("");

        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return converter;
    }

}