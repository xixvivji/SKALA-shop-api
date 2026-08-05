package com.skala.shopping.inventory;

import java.util.UUID;

public final class StockBalance {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int MAX_ORDER_QUANTITY = 1_000_000;

    private final UUID productId;
    private final int availableQuantity;
    private final boolean active;

    public StockBalance(UUID productId, int availableQuantity, boolean active) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.active = active;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getMaxOrderQuantity() {
        return Math.min(availableQuantity, MAX_ORDER_QUANTITY);
    }

    public boolean isOrderable() {
        return active && availableQuantity > 0;
    }

    public String getStockStatus() {
        if (!active) {
            return "INACTIVE";
        }
        if (availableQuantity == 0) {
            return "OUT_OF_STOCK";
        }
        if (availableQuantity <= LOW_STOCK_THRESHOLD) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }
}
