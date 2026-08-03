package com.skala.shopping.auth.internal.web;

import com.skala.shopping.auth.internal.AuthApplicationService;
import com.skala.shopping.auth.internal.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "인증", description = "고객 로그인과 로그아웃")
class AuthController {

    private final AuthApplicationService service;
    private final SecurityProperties properties;

    AuthController(AuthApplicationService service, SecurityProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "성공하면 HttpOnly JWT 쿠키를 발급합니다.")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = service.login(request.getCustomerId(), request.getCustomerPassword());
        ResponseCookie cookie = ResponseCookie.from(
                        properties.getCookie().getName(),
                        result.getAccessToken()
                )
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/")
                .maxAge(properties.getJwt().getAccessTokenTtl())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(
                        result.getMemberId(),
                        result.getLoginId(),
                        result.getExpiresAt()
                ));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookie().getName(), "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .sameSite(properties.getCookie().getSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
