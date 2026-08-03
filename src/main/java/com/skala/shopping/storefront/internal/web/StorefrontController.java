package com.skala.shopping.storefront.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.storefront.internal.CustomerDetailView;
import com.skala.shopping.storefront.internal.RegistrationView;
import com.skala.shopping.storefront.internal.StorefrontApplicationService;
import com.skala.shopping.wallet.WalletApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
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
    private final WalletApi walletApi;

    StorefrontController(StorefrontApplicationService service, WalletApi walletApi) {
        this.service = service;
        this.walletApi = walletApi;
    }

    @PostMapping
    @Operation(summary = "회원가입")
    ResponseEntity<RegistrationView> register(
            @Valid @RequestBody RegisterCustomerRequest request
    ) {
        RegistrationView registered = service.register(
                request.getCustomerId(),
                request.getCustomerPassword(),
                request.getCustomerName()
        );
        return ResponseEntity.created(
                URI.create("/api/customers/" + registered.getCustomerId())
        ).body(registered);
    }

    @GetMapping("/{customerId}")
    @Operation(
            summary = "고객 상세 조회",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    CustomerDetailView getCustomer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String customerId
    ) {
        return service.getCustomer(memberId(jwt), customerId);
    }

    @PostMapping("/order")
    @Operation(
            summary = "주문 생성 호환 API",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    OrderCompatibilityResponse placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Idempotency-Key", required = false) UUID commandId,
            @Valid @RequestBody StorefrontOrderRequest request
    ) {
        UUID memberId = memberId(jwt);
        var order = service.placeOrder(
                memberId,
                request.getProductId(),
                request.getQuantity(),
                commandId == null ? UUID.randomUUID() : commandId
        );
        return new OrderCompatibilityResponse(
                order,
                walletApi.getBalance(memberId).getBalance()
        );
    }

    @PostMapping("/cancel")
    @Operation(
            summary = "주문 취소 호환 API",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    CancellationView cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Idempotency-Key", required = false) UUID commandId,
            @Valid @RequestBody StorefrontOrderRequest request
    ) {
        return service.cancelOrder(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId == null ? UUID.randomUUID() : commandId
        );
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal Jwt jwt) {
        service.deactivate(memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
