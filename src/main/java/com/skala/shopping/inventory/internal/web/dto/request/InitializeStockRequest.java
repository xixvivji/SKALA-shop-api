package com.skala.shopping.inventory.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "InitializeStockRequest", description = "기존 상품 재고 초기화 요청")
public final class InitializeStockRequest {

    @Schema(
            description = "초기 주문 가능 재고",
            example = "100",
            minimum = "0",
            maximum = "1000000"
    )
    @Min(0)
    @Max(1_000_000)
    private int availableQuantity;

    public InitializeStockRequest() {
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
