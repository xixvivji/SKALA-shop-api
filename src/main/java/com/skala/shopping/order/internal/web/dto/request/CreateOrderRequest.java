package com.skala.shopping.order.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "CreateOrderRequest", description = "주문 생성 요청")
public final class CreateOrderRequest {

    @Schema(description = "상품 식별자")
    @NotNull
    private UUID productId;

    @Schema(description = "주문 수량", example = "1", minimum = "1")
    @Min(1)
    @Max(1_000_000)
    private int quantity;

    public CreateOrderRequest() {
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
