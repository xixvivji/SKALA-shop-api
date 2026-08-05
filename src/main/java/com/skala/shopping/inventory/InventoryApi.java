package com.skala.shopping.inventory;

import java.util.UUID;

public interface InventoryApi {

    StockBalance reserve(
            UUID productId,
            int quantity,
            UUID operationId
    );

    StockBalance release(
            UUID productId,
            int quantity,
            UUID operationId
    );
}
