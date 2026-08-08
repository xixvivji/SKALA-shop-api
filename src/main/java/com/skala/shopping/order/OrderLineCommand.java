package com.skala.shopping.order;

import java.util.UUID;

public final class OrderLineCommand {
    private final UUID productId;
    private final UUID variantId;
    private final int quantity;
    public OrderLineCommand(UUID productId, int quantity) { this(productId, null, quantity); }
    public OrderLineCommand(UUID productId, UUID variantId, int quantity) {
        this.productId = productId; this.variantId = variantId; this.quantity = quantity;
    }
    public UUID getProductId() { return productId; }
    public UUID getVariantId() { return variantId == null ? productId : variantId; }
    public int getQuantity() { return quantity; }
}
