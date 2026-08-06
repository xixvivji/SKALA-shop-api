package com.skala.shopping.order;

import java.util.UUID;

public final class OrderLineCommand {
    private final UUID productId;
    private final int quantity;
    public OrderLineCommand(UUID productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
