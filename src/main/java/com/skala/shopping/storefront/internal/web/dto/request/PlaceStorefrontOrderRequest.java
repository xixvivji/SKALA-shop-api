package com.skala.shopping.storefront.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "PlaceStorefrontOrderRequest", description = "PDF 호환 상품 주문 요청")
public final class PlaceStorefrontOrderRequest {

    @Schema(description = "상품 식별자")
    @NotNull
    private UUID productId;

    @Schema(description = "주문 수량", example = "1", minimum = "1")
    @Min(1)
    @Max(1_000_000)
    private int quantity;

    public PlaceStorefrontOrderRequest() {
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
