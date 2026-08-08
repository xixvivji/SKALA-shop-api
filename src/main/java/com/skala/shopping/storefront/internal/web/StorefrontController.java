package com.skala.shopping.storefront.internal.web;

import com.skala.shopping.auth.AuthenticationCookieApi;
import com.skala.shopping.auth.AuthenticationRateLimitApi;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.storefront.internal.StorefrontApplicationService;
import com.skala.shopping.storefront.internal.web.dto.request.CancelStorefrontOrderRequest;
import com.skala.shopping.storefront.internal.web.dto.request.PlaceStorefrontOrderRequest;
import com.skala.shopping.storefront.internal.web.dto.request.RegisterCustomerRequest;
import com.skala.shopping.storefront.internal.web.dto.request.ResetPasswordRequest;
import com.skala.shopping.storefront.internal.web.dto.response.CancellationResponse;
import com.skala.shopping.storefront.internal.web.dto.response.CustomerResponse;
import com.skala.shopping.storefront.internal.web.dto.response.OrderCompatibilityResponse;
import com.skala.shopping.storefront.internal.web.dto.response.RegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "고객 쇼핑", description = "회원가입과 고객 중심 쇼핑 API")
class StorefrontController {

    private final StorefrontApplicationService service;
    private final AuthenticationCookieApi authenticationCookieApi;
    private final AuthenticationRateLimitApi rateLimitApi;

    StorefrontController(
            StorefrontApplicationService service,
            AuthenticationCookieApi authenticationCookieApi,
            AuthenticationRateLimitApi rateLimitApi
    ) {
        this.service = service;
        this.authenticationCookieApi = authenticationCookieApi;
        this.rateLimitApi = rateLimitApi;
    }

    @PostMapping
    @Operation(
            summary = "회원가입",
            description = "고객 ID, 비밀번호와 이름을 검증해 고객 계정과 기본 포인트 지갑을 생성합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "가입된 고객",
                            content = @Content(schema = @Schema(implementation = RegistrationResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "회원가입 입력값 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "고객 ID 중복",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "회원가입 요청 제한 초과",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitApi.checkRegistration(servletRequest.getRemoteAddr(), request.getCustomerId());
        RegistrationResponse registered = RegistrationResponse.from(service.register(
                request.getCustomerId(),
                request.getCustomerPassword(),
                request.getCustomerName()
        ));
        return ResponseEntity.created(
                URI.create("/api/customers/" + registered.getCustomerId())
        ).body(registered);
    }

    @PostMapping("/password/reset")
    @Operation(
            summary = "비밀번호 재설정",
            description = "고객 ID와 현재 등록된 이름을 확인해 새 비밀번호로 변경하는 데모용 API입니다. "
                    + "운영 환경에서는 이메일 또는 휴대전화 소유 확인과 일회용 토큰 방식으로 교체해야 합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "비밀번호 재설정 완료"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 오류 또는 회원 정보 불일치",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "비밀번호 초기화 요청 제한 초과",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimitApi.checkPasswordReset(
                servletRequest.getRemoteAddr(),
                request.getCustomerId()
        );
        service.resetPassword(
                request.getCustomerId(),
                request.getCustomerName(),
                request.getNewPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "HttpOnly 인증 쿠키로 현재 로그인 세션을 복구하고 프로필, 포인트와 역할을 조회합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "현재 로그인 고객",
                            content = @Content(schema = @Schema(implementation = CustomerResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "고객 권한 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    CustomerResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return CustomerResponse.from(
                service.getCurrentCustomer(memberId(jwt)),
                role(jwt)
        );
    }

    @GetMapping("/{customerId}")
    @Operation(
            summary = "고객 상세 조회",
            description = "관리자가 고객 ID로 프로필, 포인트와 구매 내역을 상세 조회합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    CustomerResponse getCustomer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId
    ) {
        return CustomerResponse.from(
                service.getCustomer(memberId(jwt), customerId),
                role(jwt)
        );
    }

    @PostMapping("/order")
    @Operation(
            summary = "주문 생성 호환 API",
            description = "이전 단일 상품 요청 형식의 상품 ID, 수량과 필수 배송지로 포인트 주문을 멱등하게 생성합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "주문 결과",
                            content = @Content(schema = @Schema(implementation = OrderCompatibilityResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "상품·배송지 입력 오류 또는 잘못된 멱등성 키",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "고객 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "포인트 부족, 판매 불가 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    OrderCompatibilityResponse placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "주문 재시도에 동일하게 사용하는 UUID. 다른 주문 내용에 재사용하면 409를 반환합니다.",
                    required = true
            )
            @RequestHeader(name = "X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody PlaceStorefrontOrderRequest request
    ) {
        UUID memberId = memberId(jwt);
        var order = service.placeOrder(
                memberId,
                request.getProductId(),
                request.getQuantity(),
                request.getShippingAddress().toCommand(),
                commandId
        );
        return OrderCompatibilityResponse.from(
                order,
                order.getRemainingPoints()
        );
    }

    @PostMapping("/cancel")
    @Operation(
            summary = "주문 취소 호환 API",
            description = "상품 ID와 수량을 기준으로 취소하며, 같은 상품의 취소 가능 수량을 최신 주문부터 차감합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "취소 결과",
                            content = @Content(schema = @Schema(implementation = CancellationResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 또는 멱등성 키",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "고객 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "취소 수량 부족 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    CancellationResponse cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "취소 재시도에 동일하게 사용하는 UUID. 다른 취소 내용에 재사용하면 409를 반환합니다.",
                    required = true
            )
            @RequestHeader(name = "X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody CancelStorefrontOrderRequest request
    ) {
        return CancellationResponse.from(service.cancelOrder(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId
        ));
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 고객 계정을 비활성화하고 인증 세션을 종료합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(responseCode = "204", description = "회원 탈퇴 완료"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "고객 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt) {
        service.deactivate(memberId(jwt));
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authenticationCookieApi.expireAccessTokenCookie().toString()
                )
                .build();
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }

    private String role(Jwt jwt) {
        try {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
            }
            return role;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
