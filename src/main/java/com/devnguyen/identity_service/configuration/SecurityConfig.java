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
 * @Configuration: class chứa @Bean definitions
 *
 * @EnableWebSecurity: bật Spring Security cho web layer.
 * Thực ra Spring Boot auto-config đã bật rồi, nhưng khai báo tường minh
 * → Ý định rõ ràng, dễ đọc code
 *
 * @EnableMethodSecurity: bật method-level security.
 * → Cho phép dùng @PreAuthorize, @PostAuthorize, @Secured trên method
 * → Không có annotation này → @PreAuthorize bị ignore hoàn toàn!
 * → prePostEnabled = true (default): bật @PreAuthorize và @PostAuthorize
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /*
     * PUBLIC_ENDPOINTS: danh sách endpoint không cần authenticate.
     * Khai báo là constant để dễ maintain, không hardcode trong config method.
     *
     * Tại sao đây là POST endpoints?
     * → POST /users: user tự register → chưa có tài khoản → không thể authenticate
     * → POST /auth/*: login, introspect, logout, refresh → cần public
     */
    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/users",
            "/auth/token",
            "/auth/introspect",
            "/auth/logout",
            "/auth/refresh"
    };

    /*
     * Inject CustomJwtDecoder để dùng trong oauth2ResourceServer config.
     * Tại sao @Autowired thay vì constructor injection?
     * → SecurityConfig cũng cần PasswordEncoder Bean (được khai báo trong chính class này)
     * → Nếu dùng constructor injection cho CustomJwtDecoder, Spring có thể gặp
     *   vấn đề khi resolve dependencies trong cùng @Configuration class
     * → @Autowired field injection là acceptable trong @Configuration class
     *   vì @Configuration class được xử lý đặc biệt bởi Spring (CGLIB proxy)
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
     *
     * Tại sao là @Bean method thay vì extend WebSecurityConfigurerAdapter?
     * → Cách mới từ Spring Security 5.7+ (Spring Boot 3.x)
     * → Component-based: dễ test, dễ có nhiều chain
     * → WebSecurityConfigurerAdapter đã deprecated
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                /*
                 * 1. AUTHORIZATION RULES: ai được phép làm gì
                 *
                 * authorizeHttpRequests: cấu hình rules cho từng request
                 * Rules được evaluate theo THỨ TỰ khai báo → đặt rule cụ thể trước, chung sau
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Rule 1: Permit public POST endpoints (không cần token)
                         *
                         * Tại sao cần chỉ định HttpMethod.POST?
                         * → Nếu không chỉ định method: permit GET /users cũng bị permit
                         * → GET /users cần ROLE_ADMIN → không nên public
                         * → Specify method để chính xác hơn
                         *
                         * hasAnyPattern: match URL pattern với Ant-style
                         * "/users" match exact "/users"
                         * "/auth/**" match "/auth/token", "/auth/introspect", ...
                         */
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()

                        /*
                         * Rule 2: Tất cả request còn lại cần authenticate.
                         * anyRequest().authenticated():
                         * → Phải có JWT hợp lệ trong Authorization header
                         * → Nếu không → BearerTokenAuthenticationFilter reject
                         * → JwtAuthenticationEntryPoint trả về 401
                         *
                         * Lưu ý: authenticated() KHÔNG check role/permission
                         * → Chỉ check "có token hợp lệ không"
                         * → Role/permission check do @PreAuthorize trong Service đảm nhiệm
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * 2. OAUTH2 RESOURCE SERVER: cấu hình JWT processing
                 *
                 * oauth2ResourceServer: bật JWT authentication.
                 * → Spring Security tự thêm BearerTokenAuthenticationFilter vào chain
                 * → Filter này extract JWT từ "Authorization: Bearer <token>" header
                 * → Gọi JwtDecoder (CustomJwtDecoder của mình) để decode
                 * → Tạo JwtAuthenticationToken và set vào SecurityContext
                 */
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                /*
                                 * jwtDecoder: dùng CustomJwtDecoder thay vì default NimbusJwtDecoder
                                 * → CustomJwtDecoder check blacklist trước khi decode
                                 */
                                .decoder(customJwtDecoder)

                                /*
                                 * jwtAuthenticationConverter: convert JWT → Authentication object
                                 * Mặc định Spring chỉ read "scope" và "scp" claims
                                 * Custom converter để đọc đúng format scope của mình
                                 */
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )

                        /*
                         * authenticationEntryPoint: khi JWT invalid/missing → gọi handler này
                         * → Trả về JSON 401 thay vì HTML error page
                         */
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                /*
                 * 3. EXCEPTION HANDLING
                 *
                 * accessDeniedHandler: khi đã authenticate nhưng không có quyền → 403
                 * Tại sao cần config ở đây VÀ trong GlobalExceptionHandler?
                 *
                 * - Security Filter level (đây): khi exception xảy ra trong Filter Chain
                 *   TRƯỚC khi vào Controller/Service → GlobalExceptionHandler không catch được
                 * - GlobalExceptionHandler: khi exception xảy ra trong Controller/Service
                 *   SAU khi qua Filter Chain (ví dụ @PreAuthorize trong Service)
                 *
                 * Thực ra @PreAuthorize exception cũng được Spring Security intercept
                 * → accessDeniedHandler xử lý cả 2 case
                 * → GlobalExceptionHandler.handleAccessDeniedException() ít khi được gọi
                 * → Giữ lại để làm safety net
                 */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                /*
                 * 4. SESSION MANAGEMENT: STATELESS
                 *
                 * STATELESS: Spring Security không tạo/dùng HTTP session
                 * → Mỗi request phải tự mang JWT
                 * → Không có server-side state
                 * → Phù hợp với REST API + JWT
                 *
                 * Nếu không set: default là IF_REQUIRED
                 * → Spring tạo session nếu cần → có thể gây confusion với JWT auth
                 */
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /*
                 * 5. CSRF: Disable
                 *
                 * Lý do đã giải thích ở 7.3:
                 * → JWT trong Authorization header → browser không tự gửi
                 * → CSRF attack không áp dụng được
                 * → Disable để không cần gửi CSRF token trong mỗi request
                 *
                 * AbstractHttpConfigurer::disable: method reference cho disable()
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * 6. CORS: Enable với default config
                 *
                 * cors(Customizer.withDefaults()): dùng CorsConfigurationSource Bean
                 * nếu có, hoặc Spring MVC CorsConfiguration nếu không có.
                 *
                 * Trong project này dùng default → chấp nhận request từ mọi origin
                 * (không phù hợp production, nhưng ok cho development)
                 *
                 * Production: tạo CorsConfigurationSource Bean với allowed origins cụ thể
                 */
                .cors(cors -> cors.configure(http));

        return http.build();
    }

    /*
     * JwtAuthenticationConverter: convert JWT claims → Spring Security Authorities
     *
     * Tại sao cần custom converter?
     *
     * Default behavior của Spring Security OAuth2:
     * → Đọc claim "scope" hoặc "scp"
     * → Tự động thêm prefix "SCOPE_" vào mỗi value
     * → "ROLE_ADMIN" → "SCOPE_ROLE_ADMIN" ← SAI! Không match với @PreAuthorize
     *
     * Custom converter:
     * → Đọc claim "scope"
     * → KHÔNG thêm prefix (authority prefix = "")
     * → "ROLE_ADMIN" → "ROLE_ADMIN" ← ĐÚNG! Match với hasAuthority("ROLE_ADMIN")
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        /*
         * JwtGrantedAuthoritiesConverter: convert scope claim → GrantedAuthority list
         */
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        /*
         * setAuthorityPrefix(""): không thêm prefix gì cả
         * Default là "SCOPE_" → phải override thành ""
         *
         * Ví dụ không set:
         *   scope = "ROLE_ADMIN CREATE_DATA"
         *   → authorities = ["SCOPE_ROLE_ADMIN", "SCOPE_CREATE_DATA"]
         *   → @PreAuthorize("hasAuthority('ROLE_ADMIN')") → FAIL (cần "SCOPE_ROLE_ADMIN")
         *
         * Sau khi set authorityPrefix = "":
         *   scope = "ROLE_ADMIN CREATE_DATA"
         *   → authorities = ["ROLE_ADMIN", "CREATE_DATA"]
         *   → @PreAuthorize("hasAuthority('ROLE_ADMIN')") → PASS ✓
         */
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        /*
         * setAuthoritiesClaimName("scope"): đọc authorities từ claim tên "scope"
         * Default cũng là "scope", nhưng khai báo tường minh cho rõ ràng
         * Phải khớp với tên claim khi generate token:
         * .claim("scope", buildScope(user)) trong AuthenticationService
         */
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return converter;
    }

    /*
     * PasswordEncoder Bean.
     * Đặt ở đây vì liên quan đến Security configuration.
     * ApplicationInitConfig và UserService sẽ inject Bean này.
     */
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(10);
//    }
}