package com.skala.shopping.order.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.OrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "주문", description = "내 주문 생성, 조회와 취소")
@SecurityRequirement(name = "cookieAuth")
class OrderController {

    private final OrderApi orderApi;

    OrderController(OrderApi orderApi) {
        this.orderApi = orderApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "주문 생성")
    OrderView placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Idempotency-Key", required = false) UUID commandId,
            @Valid @RequestBody OrderRequest request
    ) {
        return orderApi.placeOrder(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId == null ? UUID.randomUUID() : commandId
        );
    }

    @GetMapping("/me")
    @Operation(summary = "내 주문 목록 조회")
    List<OrderView> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderApi.getOrders(memberId(jwt));
    }

    @PostMapping("/cancellations")
    @Operation(summary = "상품 부분 취소")
    CancellationView cancelProduct(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-Idempotency-Key", required = false) UUID commandId,
            @Valid @RequestBody OrderRequest request
    ) {
        return orderApi.cancelProduct(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId == null ? UUID.randomUUID() : commandId
        );
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
