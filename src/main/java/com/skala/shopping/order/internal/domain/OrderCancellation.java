package com.skala.shopping.order.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "order_cancellations",
        schema = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_cancellations_member_command",
                columnNames = {"member_id", "command_id"}
        )
)
public class OrderCancellation {

    @Id
    private UUID id;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "request_fingerprint", nullable = false, length = 128)
    private String requestFingerprint;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "canceled_at", nullable = false)
    private Instant canceledAt;

    protected OrderCancellation() {
    }

    public OrderCancellation(
            UUID id,
            UUID commandId,
            String requestFingerprint,
            UUID memberId,
            UUID productId,
            int quantity,
            BigDecimal refundAmount,
            BigDecimal balanceAfter,
            Instant canceledAt
    ) {
        this.id = id;
        this.commandId = commandId;
        this.requestFingerprint = requestFingerprint;
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.refundAmount = refundAmount;
        this.balanceAfter = balanceAfter;
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

    public BigDecimal balanceAfter() {
        return balanceAfter;
    }

    public boolean hasFingerprint(String fingerprint) {
        return requestFingerprint.equals(fingerprint);
    }
}
