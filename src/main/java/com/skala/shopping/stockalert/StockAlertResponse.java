package com.skala.shopping.stockalert;

import java.time.Instant;
import java.util.UUID;

public final class StockAlertResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final int availableQuantity;
    private final Instant subscribedAt;

    public StockAlertResponse(
            UUID id,
            UUID productId,
            String productName,
            int availableQuantity,
            Instant subscribedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.subscribedAt = subscribedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }
}
