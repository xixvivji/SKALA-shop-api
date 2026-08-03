package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

public final class OrderItemView {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int orderedQuantity;
    private final int canceledQuantity;

    public OrderItemView(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity,
            int canceledQuantity
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
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

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }
}
