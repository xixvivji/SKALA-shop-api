package com.skala.shopping.order.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="order_status_histories", schema="orders")
public class OrderStatusHistory {
    @Id private UUID id;
    @Column(name="order_id", nullable=false) private UUID orderId;
    @Enumerated(EnumType.STRING) @Column(name="from_status") private FulfillmentStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name="to_status", nullable=false) private FulfillmentStatus toStatus;
    @Column(name="changed_by") private UUID changedBy;
    @Column(name="changed_at", nullable=false) private Instant changedAt;
    protected OrderStatusHistory() { }
    public OrderStatusHistory(UUID orderId, FulfillmentStatus from, FulfillmentStatus to,
                              UUID changedBy, Instant now) {
        this.id=UUID.randomUUID(); this.orderId=orderId; this.fromStatus=from;
        this.toStatus=to; this.changedBy=changedBy; this.changedAt=now;
    }
}
