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

    // @NonFinal: khi dùng @RequiredArgsConstructor với Lombok,

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long validDuration; // seconds

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long refreshableDuration; // seconds

    // ======== LOGIN ========

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        // Tìm user theo username.

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));


          // Verify password.


        boolean authenticated = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!authenticated)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Generate JWT
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ======== GENERATE TOKEN ========

    private String generateToken(User user) {

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("devnguyen.com")
                .issueTime(new Date())
                .expirationTime(Date.from(
                        Instant.now().plus(validDuration, ChronoUnit.SECONDS)
                ))

                .jwtID(UUID.randomUUID().toString())

                .claim("scope", buildScope(user))
                .build();


        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {

            signedJWT.sign(new MACSigner(signerKey.getBytes()));

            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // ========= BUILD SCOPE =========

    private String buildScope(User user) {

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

    // ========= INTROSPECT ============

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

    //======= LOGOUT =======

    public void logout(LogoutRequest request) throws JOSEException, ParseException {

        try {

            var signToken = verifyToken(request.getToken(), true);

            // Lấy jti và expiry từ token
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
            log.info("Token invalidated: {}", jit);

        } catch (AppException e) {

            log.info("Token already expired or invalid, logout ignored");
        }
    }

    // ========== REFRESH ===========

    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws JOSEException, ParseException {

        var signedJWT = verifyToken(request.getToken(), true);

        // Lấy thông tin từ token cũ
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);

        // Bước 2: Lấy user hiện tại (cần thông tin roles mới nhất)
        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ======== VERIFY TOKEN ==========

    public SignedJWT verifyToken(String token, boolean isRefresh)
            throws JOSEException, ParseException {

        SignedJWT signedJWT = SignedJWT.parse(token);

        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
        boolean validSignature = signedJWT.verify(verifier);

        if (!validSignature)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        Date expiryTime = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant()
                .plus(refreshableDuration, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        if (!expiryTime.after(new Date()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        if (invalidatedTokenRepository.existsById(jwtId))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }
}