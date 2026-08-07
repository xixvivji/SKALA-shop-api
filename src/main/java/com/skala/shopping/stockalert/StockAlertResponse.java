package com.skala.shopping.stockalert;

import java.time.Instant;
import java.util.UUID;

public final class StockAlertResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final int availableQuantity;
    private final Instant subscribedAt;
    private final Instant notifiedAt;
    private final Integer availableQuantityAtNotification;

    public StockAlertResponse(
            UUID id,
            UUID productId,
            String productName,
            int availableQuantity,
            Instant subscribedAt,
            Instant notifiedAt,
            Integer availableQuantityAtNotification
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.subscribedAt = subscribedAt;
        this.notifiedAt = notifiedAt;
        this.availableQuantityAtNotification = availableQuantityAtNotification;
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

    public Instant getNotifiedAt() { return notifiedAt; }

    public Integer getAvailableQuantityAtNotification() {
        return availableQuantityAtNotification;
    }

    public String getStatus() {
        return notifiedAt == null ? "WAITING" : "NOTIFIED";
    }
}
