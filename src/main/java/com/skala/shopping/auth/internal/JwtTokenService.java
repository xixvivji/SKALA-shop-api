package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.internal.domain.AuthAccount;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    IssuedToken issue(AuthAccount account) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(account.id().toString())
                .claim("loginId", account.loginId())
                .claim("role", account.role().name())
                .claim("credentialVersion", account.credentialVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    static final class IssuedToken {

        private final String value;
        private final Instant expiresAt;

        IssuedToken(String value, Instant expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        String getValue() {
            return value;
        }

        Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
