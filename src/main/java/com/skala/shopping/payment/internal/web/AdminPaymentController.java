package com.skala.shopping.payment.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.payment.PaymentView;
import com.skala.shopping.payment.internal.web.dto.request.FakeWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "관리자 결제", description = "전체 결제 이력 조회와 모의 PG 결제 복구 관리")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class AdminPaymentController {

    private final PaymentApi paymentApi;

    AdminPaymentController(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    @GetMapping
    @Operation(
            summary = "전체 결제 이력 조회",
            description = "관리자가 모든 고객의 결제 상태와 처리 결과를 최신순으로 페이지 조회합니다."
    )
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<PaymentView> all(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return paymentApi.getAll(page, size);
    }

    @PostMapping("/{paymentId}/reconcile")
    @Operation(
            summary = "결제 상태 재조정",
            description = "저장된 모의 PG 거래 결과를 다시 조회해 결제와 주문 상태를 일관되게 복구합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재조정된 결제",
                    content = @Content(schema = @Schema(implementation = PaymentView.class))),
            @ApiResponse(responseCode = "400", description = "결제 ID 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "현재 상태에서 재조정할 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PaymentView reconcile(@PathVariable UUID paymentId) {
        return paymentApi.reconcile(paymentId);
    }

    @PostMapping("/fake-webhooks")
    @Operation(
            summary = "모의 PG 웹훅 처리",
            description = "교육용 Fake PG 이벤트를 수신해 중복 이벤트를 무시하고 결제 상태에 반영합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "웹훅이 반영된 결제",
                    content = @Content(schema = @Schema(implementation = PaymentView.class))),
            @ApiResponse(responseCode = "400", description = "이벤트 또는 결제 ID 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "처리할 수 없는 이벤트 상태",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    PaymentView webhook(@Valid @RequestBody FakeWebhookRequest request) {
        return paymentApi.processWebhook(
                request.getEventId(), request.getPaymentId(), request.getEventType());
    }
}
