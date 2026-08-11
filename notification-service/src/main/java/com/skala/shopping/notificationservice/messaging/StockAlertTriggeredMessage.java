package com.skala.shopping.notificationservice.messaging;

import java.time.Instant;
import java.util.UUID;

public final class StockAlertTriggeredMessage {

    private UUID subscriptionId;
    private UUID memberId;
    private UUID productId;
    private int availableQuantity;
    private Instant occurredAt;

    public StockAlertTriggeredMessage() {
    }

    public UUID getSubscriptionId() { return subscriptionId; }
    public UUID getMemberId() { return memberId; }
    public UUID getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public Instant getOccurredAt() { return occurredAt; }
}
