package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

public final class OrderItemView {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final BigDecimal paidAmount;
    private final BigDecimal refundedAmount;
    private final int orderedQuantity;
    private final int canceledQuantity;

    public OrderItemView(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            int orderedQuantity,
            int canceledQuantity
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.paidAmount = paidAmount;
        this.refundedAmount = refundedAmount;
        this.orderedQuantity = orderedQuantity;
        this.canceledQuantity = canceledQuantity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }
}
