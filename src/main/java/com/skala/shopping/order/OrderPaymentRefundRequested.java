package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requests the payment module to refund the external-payment portion of an order cancellation.
 *
 * <p>The event keeps the order module independent from the payment implementation while still
 * executing synchronously in the order transaction. A listener failure therefore rolls back the
 * order cancellation instead of leaving the order and payment ledgers inconsistent.</p>
 */
public final class OrderPaymentRefundRequested {

    private final UUID memberId;
    private final UUID orderId;
    private final BigDecimal amount;
    private final UUID commandId;

    public OrderPaymentRefundRequested(
            UUID memberId,
            UUID orderId,
            BigDecimal amount,
            UUID commandId
    ) {
        this.memberId = memberId;
        this.orderId = orderId;
        this.amount = amount;
        this.commandId = commandId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public UUID getCommandId() {
        return commandId;
    }
}
