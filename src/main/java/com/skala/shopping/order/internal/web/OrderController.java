package com.skala.shopping.order.internal.web;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.internal.web.dto.request.CancelOrderRequest;
import com.skala.shopping.order.internal.web.dto.request.CreateOrderRequest;
import com.skala.shopping.order.internal.web.dto.response.CancellationResponse;
import com.skala.shopping.order.internal.web.dto.response.OrderResponse;
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
@RequestMapping("/api/orders")
@Tag(name = "주문", description = "내 주문 생성, 조회와 취소")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "고객 권한 또는 CSRF 토큰 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))
        )
})
class OrderController {

    private final OrderApi orderApi;

    OrderController(OrderApi orderApi) {
        this.orderApi = orderApi;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "주문 생성",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "생성된 주문",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 또는 멱등성 키",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품 또는 포인트 계정을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "포인트·재고 부족, 판매 불가 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    OrderResponse placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "주문 재시도에 동일하게 사용하는 UUID. 다른 주문 내용에 재사용하면 409를 반환합니다.",
                    required = true
            )
            @RequestHeader(name = "X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return OrderResponse.from(orderApi.placeOrder(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "내 주문 목록 조회")
    PageResponse<OrderResponse> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return OrderResponse.pageFrom(orderApi.getOrders(memberId(jwt), page, size));
    }

    @PostMapping("/cancellations")
    @Operation(
            summary = "상품 부분 취소",
            description = "상품 ID와 수량을 기준으로 취소하며, 같은 상품의 취소 가능 수량을 최신 주문부터 차감합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "부분 취소 결과",
                            content = @Content(schema = @Schema(implementation = CancellationResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 또는 멱등성 키",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "취소 수량 부족 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    CancellationResponse cancelProduct(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "취소 재시도에 동일하게 사용하는 UUID. 다른 취소 내용에 재사용하면 409를 반환합니다.",
                    required = true
            )
            @RequestHeader(name = "X-Idempotency-Key") UUID commandId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return CancellationResponse.from(orderApi.cancelProduct(
                memberId(jwt),
                request.getProductId(),
                request.getQuantity(),
                commandId
        ));
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
