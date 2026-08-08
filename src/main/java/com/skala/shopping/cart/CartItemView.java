package com.skala.shopping.cart;

import java.math.BigDecimal;
import java.util.UUID;

public final class CartItemView {
    private final UUID productId;
    private final UUID variantId;
    private final String sku;
    private final String optionName;
    private final String optionValue;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final int availableQuantity;
    private final boolean orderable;

    public CartItemView(UUID productId, String productName, BigDecimal unitPrice,
                        int quantity, int availableQuantity, boolean orderable) {
        this(productId, productId, null, null, null, productName, unitPrice,
                quantity, availableQuantity, orderable);
    }

    public CartItemView(UUID productId, UUID variantId, String sku, String optionName,
                        String optionValue, String productName, BigDecimal unitPrice,
                        int quantity, int availableQuantity, boolean orderable) {
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
        this.orderable = orderable;
    }

    public UUID getProductId() { return productId; }
    public UUID getVariantId() { return variantId; }
    public String getSku() { return sku; }
    public String getOptionName() { return optionName; }
    public String getOptionValue() { return optionValue; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public boolean isOrderable() { return orderable; }
    public BigDecimal getLineAmount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
