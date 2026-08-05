package com.skala.shopping.catalog;

import java.util.UUID;

public final class ProductDeleted {

    private final UUID productId;

    public ProductDeleted(UUID productId) {
        this.productId = productId;
    }

    public UUID getProductId() {
        return productId;
    }
}
