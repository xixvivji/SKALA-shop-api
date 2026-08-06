package com.skala.shopping.inventory;

import java.util.UUID;
import java.util.List;

public interface InventoryApi {

    StockBalance getStock(UUID productId);

    List<StockBalance> getStocks(List<UUID> productIds);

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
