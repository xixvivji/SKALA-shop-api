package com.skala.shopping.auth.internal.web;

import com.skala.shopping.auth.AuthenticationCookieApi;
import com.skala.shopping.auth.AuthenticationRateLimitApi;
import com.skala.shopping.auth.internal.AuthApplicationService;
import com.skala.shopping.common.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
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
    private final AuthenticationCookieApi authenticationCookieApi;
    private final AuthenticationRateLimitApi rateLimitApi;

    AuthController(
            AuthApplicationService service,
            AuthenticationCookieApi authenticationCookieApi,
            AuthenticationRateLimitApi rateLimitApi
    ) {
        this.service = service;
        this.authenticationCookieApi = authenticationCookieApi;
        this.rateLimitApi = rateLimitApi;
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "성공하면 HttpOnly JWT 쿠키를 발급합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            content = @Content(schema = @Schema(implementation = LoginResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "로그인 입력값 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "고객 ID 또는 비밀번호 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "로그인 요청 제한 초과",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitApi.checkLogin(servletRequest.getRemoteAddr(), request.getCustomerId());
        var result = service.login(request.getCustomerId(), request.getCustomerPassword());
        var cookie = authenticationCookieApi.issueAccessTokenCookie(result.getAccessToken());
        var refreshCookie = authenticationCookieApi.issueRefreshTokenCookie(result.getRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString(), refreshCookie.toString())
                .body(new LoginResponse(
                        result.getMemberId(),
                        result.getLoginId(),
                        result.getRole(),
                        result.getExpiresAt()
                ));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "현재 리프레시 세션을 폐기하고 인증 쿠키를 만료시킵니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "인증 쿠키 삭제 완료"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<Void> logout(HttpServletRequest request) {
        return logoutSession(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "일회용 리프레시 토큰을 회전하고 새 JWT 쿠키를 발급합니다.")
    ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        var result=service.refresh(authenticationCookieApi.readRefreshToken(request));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                authenticationCookieApi.issueAccessTokenCookie(result.getAccessToken()).toString(),
                authenticationCookieApi.issueRefreshTokenCookie(result.getRefreshToken()).toString())
                .body(new LoginResponse(result.getMemberId(),result.getLoginId(),result.getRole(),result.getExpiresAt()));
    }

    private ResponseEntity<Void> logoutSession(HttpServletRequest request) {
        if(request!=null)service.revokeRefreshToken(authenticationCookieApi.readRefreshToken(request));
        var cookie = authenticationCookieApi.expireAccessTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString(),
                        authenticationCookieApi.expireRefreshTokenCookie().toString())
                .build();
    }
}
