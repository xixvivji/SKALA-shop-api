package com.skala.shopping.inventory.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.inventory.StockMovementView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.inventory.internal.InventoryApplicationService;
import com.skala.shopping.inventory.internal.web.dto.request.AdjustStockRequest;
import com.skala.shopping.inventory.internal.web.dto.request.InitializeStockRequest;
import com.skala.shopping.inventory.internal.web.dto.response.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "재고", description = "상품 재고 조회와 관리자 조정")
class InventoryController {

    private final InventoryApplicationService service;
    private final CatalogApi catalogApi;

    InventoryController(
            InventoryApplicationService service,
            CatalogApi catalogApi
    ) {
        this.service = service;
        this.catalogApi = catalogApi;
    }

    @GetMapping("/{productId}/stock")
    @Operation(
            summary = "상품 재고 조회",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "상품 재고",
                            content = @Content(schema = @Schema(implementation = StockResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품 재고를 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    StockResponse getStock(@PathVariable UUID productId) {
        return StockResponse.from(service.getStock(productId));
    }

    @GetMapping("/stocks")
    @Operation(summary = "여러 상품 재고 일괄 조회")
    List<StockResponse> getStocks(
            @RequestParam
            @NotEmpty
            @Size(max = 100)
            List<UUID> productIds
    ) {
        return service.getStocks(productIds).stream()
                .map(StockResponse::from)
                .toList();
    }

    @PostMapping("/{productId}/stock")
    @Operation(
            summary = "기존 상품 재고 초기화",
            description = "재고 모듈 도입 전에 생성된 상품을 한 번만 초기화합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "초기화된 상품 재고",
                            content = @Content(schema = @Schema(implementation = StockResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "재고 초기화 요청 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 초기화된 재고 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    StockResponse initializeStock(
            @PathVariable UUID productId,
            @RequestHeader(name = "X-Idempotency-Key") UUID operationId,
            @Valid @RequestBody InitializeStockRequest request
    ) {
        var replay = service.findInitializationReplay(
                productId,
                request.getAvailableQuantity(),
                operationId
        );
        if (replay.isPresent()) {
            return StockResponse.from(replay.get());
        }
        catalogApi.getSaleableProduct(productId);
        return StockResponse.from(service.initializeStock(
                productId,
                request.getAvailableQuantity(),
                operationId
        ));
    }

    @PostMapping("/{productId}/stock/adjustments")
    @Operation(
            summary = "상품 재고 조정",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조정된 상품 재고",
                            content = @Content(schema = @Schema(implementation = StockResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "재고 조정 요청 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "재고 부족 또는 멱등성 충돌",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    StockResponse adjustStock(
            @PathVariable UUID productId,
            @Parameter(
                    description = "재고 조정 재시도에 동일하게 사용하는 UUID",
                    required = true
            )
            @RequestHeader(name = "X-Idempotency-Key") UUID operationId,
            @Valid @RequestBody AdjustStockRequest request
    ) {
        return StockResponse.from(service.adjustStock(
                productId,
                request.getQuantityDelta(),
                request.getReason(),
                operationId
        ));
    }

    @GetMapping("/{productId}/stock/movements")
    @Operation(summary="상품 재고 변경 이력", security={@SecurityRequirement(name="cookieAuth")})
    PageResponse<StockMovementView> getMovements(@PathVariable UUID productId,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){
        return service.getMovements(productId,page,size);
    }
}
