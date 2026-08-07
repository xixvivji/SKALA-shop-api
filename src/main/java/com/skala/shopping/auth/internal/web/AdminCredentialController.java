package com.skala.shopping.auth.internal.web;

import com.skala.shopping.auth.AuthenticationCookieApi;
import com.skala.shopping.auth.internal.AuthApplicationService;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "관리자 인증", description = "관리자 자격 증명 관리")
@SecurityRequirement(name = "cookieAuth")
class AdminCredentialController {

    private final AuthApplicationService service;
    private final AuthenticationCookieApi authenticationCookieApi;

    AdminCredentialController(
            AuthApplicationService service,
            AuthenticationCookieApi authenticationCookieApi
    ) {
        this.service = service;
        this.authenticationCookieApi = authenticationCookieApi;
    }

    @PutMapping("/password")
    @Operation(
            summary = "관리자 비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤 변경합니다. 성공하면 기존 JWT를 모두 무효화하고 현재 쿠키도 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "비밀번호 변경 및 세션 무효화 완료"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "새 비밀번호 정책 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "현재 비밀번호 오류 또는 인증 만료",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeAdminPasswordRequest request
    ) {
        service.changeAdminPassword(
                accountId(jwt),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        var expiredCookie = authenticationCookieApi.expireAccessTokenCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private UUID accountId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
