package com.skala.shopping.wallet.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.wallet.PointTransactionView;
import com.skala.shopping.wallet.WalletApi;
import com.skala.shopping.wallet.WalletBalance;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/wallet/me")
class WalletController {
    private final WalletApi walletApi; WalletController(WalletApi walletApi){this.walletApi=walletApi;}
    @GetMapping WalletBalance balance(@AuthenticationPrincipal Jwt jwt){return walletApi.getBalance(id(jwt));}
    @GetMapping("/transactions") PageResponse<PointTransactionView> transactions(
            @AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return walletApi.getTransactions(id(jwt),page,size);}
    private UUID id(Jwt jwt){try{return UUID.fromString(jwt.getSubject());}catch(RuntimeException e){throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);}}
}
