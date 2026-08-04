package com.skala.shopping.auth.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CsrfTokenResponse", description = "CSRF 토큰과 전송할 헤더 이름")
final class CsrfTokenResponse {

    private final String headerName;
    private final String token;

    CsrfTokenResponse(String headerName, String token) {
        this.headerName = headerName;
        this.token = token;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getToken() {
        return token;
    }
}
