package com.skala.shopping.auth.internal.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "인증과 CSRF 보호")
class CsrfController {

    private final CsrfTokenRepository csrfTokenRepository;

    CsrfController(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    @Operation(
            summary = "CSRF 토큰 발급",
            description = "상태 변경 요청 전에 호출하고 응답 토큰을 안내된 헤더로 전송합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "CSRF 토큰",
                            content = @Content(schema = @Schema(implementation = CsrfTokenResponse.class))
                    )
            }
    )
    CsrfTokenResponse csrf(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CsrfToken csrfToken = csrfTokenRepository.loadDeferredToken(request, response).get();
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }
}
