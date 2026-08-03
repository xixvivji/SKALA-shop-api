package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

public final class PurchasedProductView {

    private final UUID productId;
    private final String productName;
    private final BigDecimal latestUnitPrice;
    private final int quantity;

    public PurchasedProductView(
            UUID productId,
            String productName,
            BigDecimal latestUnitPrice,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.latestUnitPrice = latestUnitPrice;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getLatestUnitPrice() {
        return latestUnitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
