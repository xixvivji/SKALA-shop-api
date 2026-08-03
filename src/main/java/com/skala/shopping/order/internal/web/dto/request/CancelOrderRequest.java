package com.skala.shopping.order.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "CancelOrderRequest", description = "상품 부분 취소 요청")
public final class CancelOrderRequest {

    @Schema(description = "상품 식별자")
    @NotNull
    private UUID productId;

    @Schema(description = "취소 수량", example = "1", minimum = "1")
    @Min(1)
    private int quantity;

    public CancelOrderRequest() {
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
