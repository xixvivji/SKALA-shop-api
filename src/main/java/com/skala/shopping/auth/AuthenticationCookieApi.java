package com.skala.shopping.auth;

import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationCookieApi {

    ResponseCookie issueAccessTokenCookie(String accessToken);

    ResponseCookie expireAccessTokenCookie();
    ResponseCookie issueRefreshTokenCookie(String refreshToken);
    ResponseCookie expireRefreshTokenCookie();
    String readRefreshToken(HttpServletRequest request);
}
