package com.skala.shopping.storefront.internal.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

final class StorefrontOrderRequest {

    @NotNull
    private UUID productId;

    @Min(1)
    private int quantity;

    public StorefrontOrderRequest() {
    }

    public StorefrontOrderRequest(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
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
