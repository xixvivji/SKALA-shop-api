package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

public final class CancellationView {

    private final UUID id;
    private final UUID productId;
    private final int canceledQuantity;
    private final BigDecimal refundAmount;
    private final BigDecimal remainingPoints;

    public CancellationView(
            UUID id,
            UUID productId,
            int canceledQuantity,
            BigDecimal refundAmount,
            BigDecimal remainingPoints
    ) {
        this.id = id;
        this.productId = productId;
        this.canceledQuantity = canceledQuantity;
        this.refundAmount = refundAmount;
        this.remainingPoints = remainingPoints;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }
}
