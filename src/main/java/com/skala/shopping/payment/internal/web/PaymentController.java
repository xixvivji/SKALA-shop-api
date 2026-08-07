package com.skala.shopping.payment.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.payment.PaymentView;
import com.skala.shopping.payment.internal.web.dto.request.ApproveFakePaymentRequest;
import com.skala.shopping.payment.internal.web.dto.request.PreparePaymentRequest;
import io.swagger.v3.oas.annotations.Operation;
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
class PaymentController {
    private final PaymentApi paymentApi;
    PaymentController(PaymentApi paymentApi) { this.paymentApi = paymentApi; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "결제 준비")
    PaymentView prepare(@AuthenticationPrincipal Jwt jwt,
                        @RequestHeader("X-Idempotency-Key") UUID commandId,
                        @Valid @RequestBody PreparePaymentRequest request) {
        return paymentApi.prepare(memberId(jwt), request.getOrderId(), request.getMethod(), commandId);
    }

    @PostMapping("/{paymentId}/approve")
    @Operation(summary = "Fake PG 승인", description = "4242-4242-4242-4242는 성공하며 문서의 테스트 카드로 실패를 재현합니다.")
    PaymentView approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                        @RequestHeader("X-Idempotency-Key") UUID commandId,
                        @Valid @RequestBody ApproveFakePaymentRequest request) {
        return paymentApi.approve(memberId(jwt), paymentId, request.getTestCardNumber(), commandId);
    }

    @GetMapping("/{paymentId}")
    PaymentView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentApi.get(memberId(jwt), paymentId);
    }

    @GetMapping("/orders/{orderId}")
    PaymentView getByOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return paymentApi.getByOrder(memberId(jwt), orderId);
    }

    @GetMapping("/me")
    PageResponse<PaymentView> mine(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return paymentApi.getMine(memberId(jwt), page, size);
    }

    private UUID memberId(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException exception) { throw new BusinessException(ErrorCode.NOT_AUTHENTICATED); }
    }
}
