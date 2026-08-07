package com.skala.shopping.stockalert.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.stockalert.StockAlertApi;
import com.skala.shopping.stockalert.StockAlertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-alerts")
@Tag(name = "재입고 알림", description = "재입고 알림 구독/해지/조회")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class StockAlertController {

    private final StockAlertApi stockAlertApi;

    StockAlertController(StockAlertApi stockAlertApi) {
        this.stockAlertApi = stockAlertApi;
    }

    @GetMapping
    @Operation(summary = "내 재입고 알림 목록")
    PageResponse<StockAlertResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return stockAlertApi.getSubscriptions(memberId(jwt), page, size);
    }

    @PostMapping("/{productId}")
    @Operation(summary = "재입고 알림 구독")
    StockAlertResponse subscribe(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        return stockAlertApi.subscribe(memberId(jwt), productId);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "재입고 알림 해지")
    void unsubscribe(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        stockAlertApi.unsubscribe(memberId(jwt), productId);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
