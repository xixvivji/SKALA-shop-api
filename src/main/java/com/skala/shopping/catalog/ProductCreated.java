package com.skala.shopping.catalog;

import java.util.UUID;

public final class ProductCreated {

    private final UUID productId;
    private final int initialQuantity;

    public ProductCreated(UUID productId, int initialQuantity) {
        this.productId = productId;
        this.initialQuantity = initialQuantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getInitialQuantity() {
        return initialQuantity;
    }
}
