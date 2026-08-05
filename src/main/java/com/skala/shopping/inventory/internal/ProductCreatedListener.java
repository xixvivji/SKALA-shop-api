package com.skala.shopping.inventory.internal;

import com.skala.shopping.catalog.ProductCreated;
import com.skala.shopping.catalog.ProductDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ProductCreatedListener {

    private final InventoryApplicationService inventoryService;

    ProductCreatedListener(InventoryApplicationService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    void initializeStock(ProductCreated event) {
        inventoryService.initializeStock(
                event.getProductId(),
                event.getInitialQuantity(),
                event.getProductId()
        );
    }

    @EventListener
    void deactivateStock(ProductDeleted event) {
        inventoryService.deactivateStock(event.getProductId());
    }
}
