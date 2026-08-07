package com.skala.shopping.payment.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refunds", schema = "payment")
public class PaymentRefund {
    @Id private UUID id;
    @Column(name = "payment_id", nullable = false) private UUID paymentId;
    @Column(name = "command_id", nullable = false, unique = true) private UUID commandId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected PaymentRefund() { }
    public PaymentRefund(UUID paymentId, UUID commandId, BigDecimal amount, Instant now) {
        this.id = UUID.randomUUID(); this.paymentId = paymentId;
        this.commandId = commandId; this.amount = amount; this.createdAt = now;
    }
    public boolean matches(UUID expectedPaymentId, BigDecimal expectedAmount) {
        return paymentId.equals(expectedPaymentId) && amount.compareTo(expectedAmount) == 0;
    }
}
