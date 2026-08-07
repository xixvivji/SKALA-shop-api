package com.skala.shopping.wallet.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.wallet.PointTransactionView;
import com.skala.shopping.wallet.WalletApi;
import com.skala.shopping.wallet.WalletBalance;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet/me")
@Tag(name = "포인트", description = "내 포인트 잔액과 거래 내역")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class WalletController {

    private final WalletApi walletApi;

    WalletController(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    @GetMapping
    @Operation(summary = "내 포인트 잔액 조회")
    WalletBalance balance(@AuthenticationPrincipal Jwt jwt) {
        return walletApi.getBalance(memberId(jwt));
    }

    @GetMapping("/transactions")
    @Operation(summary = "내 포인트 거래 내역 조회")
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<PointTransactionView> transactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return walletApi.getTransactions(memberId(jwt), page, size);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
