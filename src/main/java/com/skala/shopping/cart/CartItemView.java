package com.skala.shopping.cart;

import java.math.BigDecimal;
import java.util.UUID;

public final class CartItemView {
    private final UUID productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final int availableQuantity;
    private final boolean orderable;

    public CartItemView(UUID productId, String productName, BigDecimal unitPrice,
                        int quantity, int availableQuantity, boolean orderable) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
        this.orderable = orderable;
    }

    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public boolean isOrderable() { return orderable; }
    public BigDecimal getLineAmount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
