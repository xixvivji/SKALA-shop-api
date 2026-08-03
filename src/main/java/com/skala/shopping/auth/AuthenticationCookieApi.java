package com.skala.shopping.auth;

import org.springframework.http.ResponseCookie;

public interface AuthenticationCookieApi {

    ResponseCookie issueAccessTokenCookie(String accessToken);

    ResponseCookie expireAccessTokenCookie();
}
