package com.skala.shopping.inventory.internal.web.dto.response;

import com.skala.shopping.inventory.StockBalance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "StockResponse", description = "상품 주문 가능 재고 응답")
public final class StockResponse {

    @Schema(description = "상품 식별자")
    private final UUID productId;

    @Schema(description = "현재 주문 가능 수량", example = "12")
    private final int availableQuantity;

    @Schema(description = "한 번에 주문 가능한 최대 수량", example = "12")
    private final int maxOrderQuantity;

    @Schema(description = "현재 주문 가능 여부", example = "true")
    private final boolean orderable;

    @Schema(description = "재고 상태", example = "IN_STOCK")
    private final String stockStatus;

    public StockResponse(
            UUID productId,
            int availableQuantity,
            int maxOrderQuantity,
            boolean orderable,
            String stockStatus
    ) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.maxOrderQuantity = maxOrderQuantity;
        this.orderable = orderable;
        this.stockStatus = stockStatus;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public boolean isOrderable() {
        return orderable;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public static StockResponse from(StockBalance stock) {
        return new StockResponse(
                stock.getProductId(),
                stock.getAvailableQuantity(),
                stock.getMaxOrderQuantity(),
                stock.isOrderable(),
                stock.getStockStatus()
        );
    }
}
