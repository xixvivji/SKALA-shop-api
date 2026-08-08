package com.skala.shopping.returns.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.web.dto.request.CreateReturnRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/returns")
@Tag(name = "반품", description = "내 주문 상품의 반품 신청과 처리 내역 조회")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class ReturnController {

    private final ReturnApi returnApi;

    ReturnController(ReturnApi returnApi) {
        this.returnApi = returnApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "반품 신청",
            description = "배송된 내 주문 항목의 수량, 사유와 선택 증빙 이미지 URL을 멱등하게 접수합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "접수된 반품 신청",
                    content = @Content(schema = @Schema(implementation = ReturnView.class))),
            @ApiResponse(responseCode = "400", description = "주문 항목, 수량, 사유 또는 멱등성 키 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "주문 또는 주문 항목을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "반품 가능 상태·수량 오류 또는 멱등성 충돌",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    ReturnView request(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "반품 신청 재시도에 동일하게 사용하는 UUID", required = true)
            @RequestHeader("X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        return returnApi.request(
                memberId(jwt),
                request.getOrderId(),
                request.getOrderItemId(),
                request.getQuantity(),
                request.getReason(),
                request.getEvidenceImageUrl(),
                commandId
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 반품 내역 조회",
            description = "로그인한 고객이 신청한 반품과 현재 처리 상태를 최신순으로 페이지 조회합니다."
    )
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<ReturnView> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return returnApi.getMine(memberId(jwt), page, size);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
