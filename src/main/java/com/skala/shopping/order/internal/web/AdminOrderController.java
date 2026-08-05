package com.skala.shopping.order.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.internal.OrderApplicationService;
import com.skala.shopping.order.internal.web.dto.request.UpdateFulfillmentRequest;
import com.skala.shopping.order.internal.web.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name="관리자 주문", description="전체 주문과 배송 상태 관리")
@SecurityRequirement(name="cookieAuth")
class AdminOrderController {
    private final OrderApplicationService service;
    AdminOrderController(OrderApplicationService service) { this.service=service; }
    @GetMapping @Operation(summary="전체 주문 조회")
    PageResponse<OrderResponse> orders(@RequestParam(defaultValue="0") @Min(0) int page,
                                       @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return OrderResponse.pageFrom(service.getAllOrders(page, size));
    }
    @PutMapping("/{orderId}/fulfillment") @Operation(summary="배송 상태 변경")
    OrderResponse fulfillment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
                              @Valid @RequestBody UpdateFulfillmentRequest request) {
        return OrderResponse.from(service.changeFulfillment(adminId(jwt), orderId, request.getStatus()));
    }
    private UUID adminId(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException exception) { throw new BusinessException(ErrorCode.NOT_AUTHENTICATED); }
    }
}
