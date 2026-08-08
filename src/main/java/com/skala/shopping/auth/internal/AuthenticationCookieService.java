package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthenticationCookieApi;
import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
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

    @Override
    public ResponseCookie issueRefreshTokenCookie(String refreshToken) {
        return refreshCookie(refreshToken).maxAge(properties.getJwt().getRefreshTokenTtl()).build();
    }

    @Override
    public ResponseCookie expireRefreshTokenCookie() {
        return refreshCookie("").maxAge(Duration.ZERO).build();
    }

    @Override
    public String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies=request.getCookies();
        if(cookies==null)return null;
        return Arrays.stream(cookies).filter(cookie -> properties.getCookie().getRefreshName().equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(properties.getCookie().getName(), value)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/");
    }

    private ResponseCookie.ResponseCookieBuilder refreshCookie(String value) {
        return ResponseCookie.from(properties.getCookie().getRefreshName(), value)
                .httpOnly(true).secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite()).path("/api/customers");
    }
}
