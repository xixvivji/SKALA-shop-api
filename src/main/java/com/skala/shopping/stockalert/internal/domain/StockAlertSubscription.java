package com.skala.shopping.stockalert.internal.domain;

import com.skala.shopping.stockalert.StockAlertResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_alert_subscriptions", schema = "stockalert")
public class StockAlertSubscription {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "available_quantity_at_notification")
    private Integer availableQuantityAtNotification;

    @Version
    private long version;

    protected StockAlertSubscription() {
    }

    public StockAlertSubscription(UUID memberId, UUID productId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID memberId() {
        return memberId;
    }

    public UUID productId() {
        return productId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant notifiedAt() { return notifiedAt; }

    public boolean markNotified(int availableQuantity, Instant notifiedAt) {
        if (this.notifiedAt != null) {
            return false;
        }
        this.notifiedAt = notifiedAt;
        this.availableQuantityAtNotification = availableQuantity;
        return true;
    }

    public void rearm() {
        this.notifiedAt = null;
        this.availableQuantityAtNotification = null;
    }

    public StockAlertResponse toResponse(String productName, int availableQuantity) {
        return new StockAlertResponse(
                id,
                productId,
                productName,
                availableQuantity,
                createdAt,
                notifiedAt,
                availableQuantityAtNotification
        );
    }
}
