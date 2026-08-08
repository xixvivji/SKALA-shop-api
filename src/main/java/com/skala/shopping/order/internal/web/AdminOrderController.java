package com.skala.shopping.order.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.internal.OrderApplicationService;
import com.skala.shopping.order.internal.web.dto.request.UpdateFulfillmentRequest;
import com.skala.shopping.order.internal.web.dto.response.OrderResponse;
import com.skala.shopping.order.OrderStatusHistoryView;
import java.util.List;
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
@Tag(name = "관리자 주문", description = "전체 주문과 배송 상태 관리")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class AdminOrderController {

    private final OrderApplicationService service;

    AdminOrderController(OrderApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "전체 주문 조회", description = "관리자가 모든 고객의 주문을 최신순으로 페이지 조회합니다.")
    @ApiResponse(responseCode = "400", description = "페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<OrderResponse> orders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return OrderResponse.pageFrom(service.getAllOrders(page, size));
    }

    @PutMapping("/{orderId}/fulfillment")
    @Operation(summary = "배송 상태 변경", description = "관리자가 주문의 배송 상태와 운송장 정보를 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "주문 ID 또는 배송 상태 전이 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    OrderResponse fulfillment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateFulfillmentRequest request
    ) {
        return OrderResponse.from(service.changeFulfillment(
                adminId(jwt),
                orderId,
                request.getStatus(),
                request.getTrackingCarrier(),
                request.getTrackingNumber(),
                request.getTrackingUrl(),
                request.getEstimatedDeliveryAt()
        ));
    }

    @GetMapping("/{orderId}/history")
    @Operation(summary = "배송 상태 변경 이력", description = "주문의 배송 상태 변경 기록을 시간순으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "주문 ID 오류",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    List<OrderStatusHistoryView> history(@PathVariable UUID orderId) {
        return service.getStatusHistory(orderId);
    }

    private UUID adminId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
