package com.skala.shopping.order.internal;

import com.skala.shopping.inventory.InventoryApi;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalStockManager implements StockManager {

    private final InventoryApi inventoryApi;

    LocalStockManager(InventoryApi inventoryApi) {
        this.inventoryApi = inventoryApi;
    }

    @Override
    public int reserve(UUID productId, int quantity, UUID operationId) {
        return inventoryApi.reserve(productId, quantity, operationId).getAvailableQuantity();
    }

    @Override
    public int release(UUID productId, int quantity, UUID operationId) {
        return inventoryApi.release(productId, quantity, operationId).getAvailableQuantity();
    }
}
