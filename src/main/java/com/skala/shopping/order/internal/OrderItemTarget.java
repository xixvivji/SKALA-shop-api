package com.skala.shopping.order.internal;

import java.util.UUID;

/** Lightweight cancellation target that does not put a stale OrderItem entity in the persistence context. */
final class OrderItemTarget {

    private final UUID itemId;
    private final UUID orderId;
    private final UUID productId;
    private final UUID variantId;

    OrderItemTarget(UUID itemId, UUID orderId, UUID productId, UUID variantId) {
        this.itemId = itemId;
        this.orderId = orderId;
        this.productId = productId;
        this.variantId = variantId;
    }

    UUID itemId() { return itemId; }
    UUID orderId() { return orderId; }
    UUID productId() { return productId; }
    UUID variantId() { return variantId; }
}
