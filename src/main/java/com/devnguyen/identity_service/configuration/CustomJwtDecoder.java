package com.devnguyen.identity_service.configuration;

import com.devnguyen.identity_service.dto.request.IntrospectRequest;
import com.devnguyen.identity_service.exception.AppException;
import com.devnguyen.identity_service.exception.ErrorCode;
import com.devnguyen.identity_service.service.AuthenticationService;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    private String signerKey;

    /*
     @Autowired + @Lazy: giải quyết circular dependency.
      @Lazy: Spring không inject AuthenticationService ngay khi tạo CustomJwtDecoder
     */
    @Autowired
    @Lazy
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {

        try {
            var response = authenticationService.introspect(
                    IntrospectRequest.builder().token(token).build()
            );

            if (!response.isAuthenticated())
                throw new AppException(ErrorCode.UNAUTHENTICATED);

        } catch (AppException e) {
            throw new JwtException(e.getMessage());
        } catch (Exception e) {
            throw new JwtException(e.getMessage());
        }

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    signerKey.getBytes(),
                    "HmacSHA512"
            );
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}