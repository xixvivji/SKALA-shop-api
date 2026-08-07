package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.internal.domain.AccountRole;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class ActiveAccountJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private final AuthAccountRepository repository;

    ActiveAccountJwtAuthenticationConverter(AuthAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        UUID accountId = accountId(jwt);
        // 서명이 유효해도 탈퇴·비활성화된 계정의 과거 토큰은 매 요청마다 거부합니다.
        var account = repository.findById(accountId)
                .filter(candidate -> candidate.isActive())
                .orElseThrow(() -> new BadCredentialsException("Active account not found"));

        AccountRole tokenRole = tokenRole(jwt);
        if (account.role() != tokenRole) {
            throw new BadCredentialsException("Account role has changed");
        }
        // 비밀번호 변경 시 credentialVersion이 증가하므로 이전에 발급한 JWT가 즉시 무효화됩니다.
        if (account.credentialVersion() != credentialVersion(jwt)) {
            throw new BadCredentialsException("Account credentials have changed");
        }

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + tokenRole.name()));
        return new JwtAuthenticationToken(jwt, authorities, account.loginId());
    }

    private UUID accountId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Invalid JWT subject", exception);
        }
    }

    private AccountRole tokenRole(Jwt jwt) {
        try {
            return AccountRole.valueOf(jwt.getClaimAsString("role"));
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Invalid JWT role", exception);
        }
    }

    private long credentialVersion(Jwt jwt) {
        Object claim = jwt.getClaim("credentialVersion");
        if (claim instanceof Number number) {
            return number.longValue();
        }
        throw new BadCredentialsException("Invalid JWT credential version");
    }
}
