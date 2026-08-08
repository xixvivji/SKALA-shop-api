package com.skala.shopping.payment.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.payment.PaymentView;
import com.skala.shopping.payment.internal.web.dto.request.ApproveFakePaymentRequest;
import com.skala.shopping.payment.internal.web.dto.request.PreparePaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "모의 결제", description = "실제 금전 이동이 없는 Fake PG 결제")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class PaymentController {

    private final PaymentApi paymentApi;

    PaymentController(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "결제 준비",
            description = "내 주문의 결제 수단과 금액을 확인하고 모의 결제 건을 멱등하게 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "준비된 결제",
                    content = @Content(schema = @Schema(implementation = PaymentView.class))),
            @ApiResponse(responseCode = "400", description = "주문, 결제 수단 또는 멱등성 키 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "결제 준비 불가 또는 멱등성 충돌",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PaymentView prepare(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "결제 준비 재시도에 동일하게 사용하는 UUID", required = true)
            @RequestHeader("X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody PreparePaymentRequest request
    ) {
        return paymentApi.prepare(memberId(jwt), request.getOrderId(), request.getMethod(), commandId);
    }

    @PostMapping("/{paymentId}/approve")
    @Operation(
            summary = "모의 PG 결제 승인",
            description = "4242-4242-4242-4242는 성공하며 문서에 안내된 테스트 카드로 실패를 재현합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인된 결제",
                    content = @Content(schema = @Schema(implementation = PaymentView.class))),
            @ApiResponse(responseCode = "400", description = "결제 ID, 테스트 카드 또는 멱등성 키 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "결제 승인 불가 또는 멱등성 충돌",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PaymentView approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID paymentId,
            @Parameter(description = "결제 승인 재시도에 동일하게 사용하는 UUID", required = true)
            @RequestHeader("X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody ApproveFakePaymentRequest request
    ) {
        return paymentApi.approve(memberId(jwt), paymentId, request.getTestCardNumber(), commandId);
    }

    @GetMapping("/{paymentId}")
    @Operation(
            summary = "결제 상세 조회",
            description = "로그인한 고객이 소유한 결제 건의 상태와 처리 원장을 조회합니다."
    )
    @ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PaymentView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentApi.get(memberId(jwt), paymentId);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "주문별 결제 조회",
            description = "로그인한 고객의 주문 ID로 연결된 결제 상태와 처리 결과를 조회합니다."
    )
    @ApiResponse(responseCode = "404", description = "주문 또는 결제를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PaymentView getByOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return paymentApi.getByOrder(memberId(jwt), orderId);
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 결제 내역 조회",
            description = "로그인한 고객의 결제 준비·승인·실패·환불 이력을 최신순으로 페이지 조회합니다."
    )
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<PaymentView> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return paymentApi.getMine(memberId(jwt), page, size);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
