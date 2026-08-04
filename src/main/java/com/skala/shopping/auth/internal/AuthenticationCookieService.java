package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthenticationCookieApi;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class AuthenticationCookieService implements AuthenticationCookieApi {

    private final SecurityProperties properties;

    AuthenticationCookieService(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResponseCookie issueAccessTokenCookie(String accessToken) {
        return cookie(accessToken)
                .maxAge(properties.getJwt().getAccessTokenTtl())
                .build();
    }

    @Override
    public ResponseCookie expireAccessTokenCookie() {
        return cookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(properties.getCookie().getName(), value)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/");
    }
}
