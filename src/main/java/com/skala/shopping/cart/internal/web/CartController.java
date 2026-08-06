package com.skala.shopping.cart.internal.web;

import com.skala.shopping.cart.CartView;
import com.skala.shopping.cart.internal.CartApplicationService;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "장바구니", description = "내 장바구니 관리")
@SecurityRequirement(name = "cookieAuth")
class CartController {
    private final CartApplicationService service;
    CartController(CartApplicationService service) { this.service = service; }

    @GetMapping @Operation(summary = "장바구니 조회")
    CartView get(@AuthenticationPrincipal Jwt jwt) { return service.getCart(memberId(jwt)); }

    @PostMapping("/items") @Operation(summary = "장바구니 상품 추가")
    CartView add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) {
        return service.addItem(memberId(jwt), request.getProductId(), request.getQuantity());
    }

    @PutMapping("/items/{productId}") @Operation(summary = "장바구니 상품 수량 변경")
    CartView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId,
                    @Valid @RequestBody UpdateCartItemRequest request) {
        return service.updateItem(memberId(jwt), productId, request.getQuantity());
    }

    @DeleteMapping("/items/{productId}") @Operation(summary = "장바구니 상품 삭제")
    CartView remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return service.removeItem(memberId(jwt), productId);
    }

    @DeleteMapping @Operation(summary = "장바구니 비우기")
    CartView clear(@AuthenticationPrincipal Jwt jwt) { return service.clearCart(memberId(jwt)); }

    private UUID memberId(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException exception) { throw new BusinessException(ErrorCode.NOT_AUTHENTICATED); }
    }
}
