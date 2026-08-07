package com.skala.shopping.payment.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events", schema = "payment")
public class PaymentWebhookEvent {
    @Id private UUID eventId;
    @Column(name = "payment_id", nullable = false) private UUID paymentId;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    protected PaymentWebhookEvent() { }
    public PaymentWebhookEvent(UUID eventId, UUID paymentId, String eventType, Instant now) {
        this.eventId = eventId; this.paymentId = paymentId;
        this.eventType = eventType; this.processedAt = now;
    }
    public boolean matches(UUID expectedPaymentId, String expectedType) {
        return paymentId.equals(expectedPaymentId) && eventType.equals(expectedType);
    }
}
