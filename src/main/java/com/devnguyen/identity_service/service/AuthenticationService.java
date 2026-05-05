package com.devnguyen.identity_service.service;

import com.devnguyen.identity_service.dto.request.*;
import com.devnguyen.identity_service.dto.response.AuthenticationResponse;
import com.devnguyen.identity_service.entity.InvalidatedToken;
import com.devnguyen.identity_service.entity.User;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.repository.InvalidatedTokenRepository;
import com.devnguyen.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /*
     * @NonFinal: khi dùng @RequiredArgsConstructor với Lombok,
     * tất cả field final sẽ được inject qua constructor.
     * @Value inject SAU constructor → signerKey sẽ là null trong constructor.
     * @NonFinal: báo Lombok KHÔNG include field này vào constructor
     * → Spring sẽ inject giá trị qua @Value sau khi object được tạo.
     *
     * Tại sao không dùng @Value trong constructor?
     * → @Value injection xảy ra sau khi Bean được tạo xong
     * → Constructor không thể nhận @Value trực tiếp (phức tạp hơn)
     * → @NonFinal + field @Value là pattern phổ biến với Lombok
     */
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long validDuration; // seconds

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long refreshableDuration; // seconds

    // ==================== LOGIN ====================

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        /*
         * Bước 1: Tìm user theo username.
         * Tại sao không throw "Wrong password" khi username không tồn tại?
         * → Security best practice: không nên tiết lộ username có tồn tại hay không
         * → "USER_NOT_EXISTED" vs "WRONG_PASSWORD" → attacker biết username đúng/sai
         * → Trong thực tế production: nên throw cùng 1 message "Invalid credentials"
         *   cho cả 2 trường hợp (username sai và password sai)
         * → Project này tách ra để dễ debug/học
         */
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        /*
         * Bước 2: Verify password.
         * passwordEncoder.matches(raw, encoded):
         * → BCrypt extract salt từ encoded hash
         * → Hash raw password với salt đó
         * → So sánh với encoded hash
         * → Không cần biết salt là gì (BCrypt tự lưu salt trong hash string)
         */
        boolean authenticated = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Bước 3: Generate JWT
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ==================== GENERATE TOKEN ====================

    private String generateToken(User user) {
        /*
         * Nimbus JOSE + JWT library:
         * Đây là library được dùng rộng rãi để làm việc với JWT trong Java.
         * Spring Security OAuth2 cũng dùng Nimbus bên dưới.
         *
         * JWSHeader: header của JWT
         * JWSAlgorithm.HS512: HMAC với SHA-512
         * Tại sao HS512 thay vì HS256?
         * → SHA-512 output dài hơn (512 bits vs 256 bits)
         * → An toàn hơn trước collision attack
         * → Với hardware hiện đại, performance difference không đáng kể
         */
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        /*
         * JWTClaimsSet: payload của JWT (các claims)
         *
         * Standard claims (được định nghĩa trong RFC 7519):
         * - subject (sub): định danh principal (thường là username hoặc userId)
         * - issuer (iss): ai tạo ra token (URL của service)
         * - issueTime (iat): thời điểm tạo token
         * - expirationTime (exp): thời điểm hết hạn
         * - jwtID (jti): unique identifier của token
         *
         * Custom claims:
         * - scope: quyền hạn của user (Spring Security đọc claim này)
         */
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("devnguyen.com")
                .issueTime(new Date())
                .expirationTime(Date.from(
                        Instant.now().plus(validDuration, ChronoUnit.SECONDS)
                ))
                /*
                 * jwtID: UUID ngẫu nhiên, unique cho mỗi token.
                 * Dùng để:
                 * 1. Blacklist khi logout (lưu jti vào InvalidatedToken)
                 * 2. Prevent replay attack
                 */
                .jwtID(UUID.randomUUID().toString())
                /*
                 * scope: build chuỗi quyền hạn theo format Spring Security expect.
                 * "ROLE_ADMIN ROLE_USER CREATE_DATA READ_DATA"
                 * → Spring Security parse thành List<GrantedAuthority>
                 */
                .claim("scope", buildScope(user))
                .build();

        /*
         * SignedJWT: JWT với chữ ký.
         * Payload: wrap claimsSet thành JWTClaimsSet → Payload
         */
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {
            /*
             * MACSigner: ký bằng HMAC (symmetric).
             * signerKey.getBytes(): chuyển String key thành byte array.
             *
             * Lưu ý: signerKey nên đủ dài (≥ 512 bits = 64 bytes cho HS512)
             * Key trong project này là hex string 64 ký tự = 64 bytes ✓
             *
             * sign(): thực hiện ký → thêm signature vào JWT
             */
            signedJWT.sign(new MACSigner(signerKey.getBytes()));

            /*
             * serialize(): chuyển SignedJWT thành String format:
             * "header.payload.signature" (Base64URL encoded)
             */
            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // ==================== BUILD SCOPE ====================

    private String buildScope(User user) {
        /*
         * StringJoiner: build chuỗi với delimiter " " (space).
         * Tại sao space? Spring Security mặc định parse scope bằng space delimiter.
         *
         * Format output: "ROLE_ADMIN ROLE_USER CREATE_DATA READ_DATA"
         *
         * Tại sao prefix "ROLE_" cho role?
         * → Spring Security convention:
         *   hasRole("ADMIN")      → check "ROLE_ADMIN" trong authorities
         *   hasAuthority("ADMIN") → check "ADMIN" exact
         * → Prefix ROLE_ để phân biệt Role và Permission trong scope
         * → Nhất quán với Spring Security expectations
         */
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                // Thêm role với prefix ROLE_
                stringJoiner.add("ROLE_" + role.getName());

                // Thêm từng permission của role này
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(permission ->
                            stringJoiner.add(permission.getName())
                    );
                }
            });
        }

        return stringJoiner.toString();
    }

    // ==================== INTROSPECT ====================

    public AuthenticationResponse introspect(IntrospectRequest request)
            throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        } catch (ParseException | JOSEException e) {
            // ← THÊM: catch luôn ParseException và JOSEException
            // Token rác, không parse được → cũng là invalid
            isValid = false;
        }

        return AuthenticationResponse.builder()
                .authenticated(isValid)
                .build();
    }

    // ==================== LOGOUT ====================

    public void logout(LogoutRequest request) throws JOSEException, ParseException {

        try {
            /*
             * Verify token trước khi logout.
             * Tại sao? Không muốn blacklist token rác.
             * isRefresh = true: cho phép logout cả token hết hạn
             * (user muốn logout dù token đã expired → vẫn nên invalidate)
             */
            var signToken = verifyToken(request.getToken(), true);

            // Lấy jti và expiry từ token
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            /*
             * Lưu jti vào blacklist.
             * Từ giờ, mỗi request với token này sẽ bị reject bởi CustomJwtDecoder.
             */
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Token invalidated: {}", jit);

        } catch (AppException e) {
            /*
             * Token đã hết hạn hoặc không hợp lệ → không cần blacklist.
             * Token hết hạn tự nhiên bị reject → không cần lưu.
             * Log để biết, nhưng không throw exception (logout "thành công" về mặt UX)
             */
            log.info("Token already expired or invalid, logout ignored");
        }
    }

    // ==================== REFRESH ====================

    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws JOSEException, ParseException {

        /*
         * Verify token với refreshable-duration.
         * isRefresh = true → check trong khoảng refreshable-duration (10h)
         * thay vì valid-duration (1h)
         * → Cho phép refresh token đã expired nhưng chưa quá 10h
         */
        var signedJWT = verifyToken(request.getToken(), true);

        // Lấy thông tin từ token cũ
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        /*
         * Bước 1: Invalidate token cũ.
         * Tại sao? Prevent token reuse:
         * → Nếu không invalidate → cùng 1 token có thể refresh nhiều lần
         * → Mỗi refresh phải đổi token mới → token cũ không dùng được nữa
         */
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);

        // Bước 2: Lấy user hiện tại (cần thông tin roles mới nhất)
        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        /*
         * Bước 3: Generate token mới.
         * Token mới có roles/permissions mới nhất của user.
         * Ví dụ: trong lúc token cũ còn hạn, admin gán thêm role cho user
         * → Token cũ chưa có role mới → Refresh → token mới có role đó
         */
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ==================== VERIFY TOKEN ====================

    /*
     * verifyToken: parse và verify token.
     *
     * @param isRefresh: true → dùng refreshable-duration để check expiry
     *                   false → dùng valid-duration (standard check)
     *
     * Tại sao cần isRefresh flag?
     * → Khi refresh: token có thể đã expired (1h) nhưng vẫn trong refreshable window (10h)
     * → Cần verify với duration dài hơn
     * → Nếu dùng standard verify → token expired 1h sẽ bị reject → không refresh được
     */
    public SignedJWT verifyToken(String token, boolean isRefresh)
            throws JOSEException, ParseException {

        // Bước 1: Parse token string thành SignedJWT object
        SignedJWT signedJWT = SignedJWT.parse(token);

        /*
         * Bước 2: Verify chữ ký.
         * MACVerifier: verify HMAC signature với cùng secret key.
         * verify() trả true nếu chữ ký hợp lệ.
         *
         * Nếu token bị giả mạo (payload bị sửa) → chữ ký sai → verify fail
         */
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
        boolean validSignature = signedJWT.verify(verifier);

        if (!validSignature)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        /*
         * Bước 3: Check expiry time.
         *
         * isRefresh = true:
         *   → Tạo "refreshable expiry" = issueTime + refreshable-duration
         *   → Check token còn trong refreshable window không
         *   → Token expired 1h nhưng issueTime + 10h > now → vẫn cho refresh
         *
         * isRefresh = false:
         *   → Dùng expirationTime có trong token (= issueTime + valid-duration)
         *   → after(new Date()): expirationTime phải SAU thời điểm hiện tại
         */
        Date expiryTime = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant()
                .plus(refreshableDuration, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        if (!expiryTime.after(new Date()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        /*
         * Bước 4: Check blacklist.
         * Nếu jti có trong bảng InvalidatedToken → token đã logout → reject.
         */
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        if (invalidatedTokenRepository.existsById(jwtId))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }
}