package com.skala.shopping.order.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_cancellations", schema = "orders")
public class OrderCancellation {

    @Id
    private UUID id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "canceled_at", nullable = false)
    private Instant canceledAt;

    protected OrderCancellation() {
    }

    public OrderCancellation(
            UUID id,
            UUID commandId,
            UUID memberId,
            UUID productId,
            int quantity,
            BigDecimal refundAmount,
            Instant canceledAt
    ) {
        this.id = id;
        this.commandId = commandId;
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.refundAmount = refundAmount;
        this.canceledAt = canceledAt;
    }

    public UUID id() {
        return id;
    }

    public UUID productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal refundAmount() {
        return refundAmount;
    }
}
