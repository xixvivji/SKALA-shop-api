package com.skala.shopping.notificationservice.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class OrderPlacedMessage {

    private UUID orderId;
    private UUID memberId;
    private BigDecimal totalAmount;
    private Instant occurredAt;

    public OrderPlacedMessage() {
    }

    public UUID getOrderId() { return orderId; }
    public UUID getMemberId() { return memberId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getOccurredAt() { return occurredAt; }
}
