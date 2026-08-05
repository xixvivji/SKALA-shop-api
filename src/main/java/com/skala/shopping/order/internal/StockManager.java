package com.skala.shopping.order.internal;

import java.util.UUID;

interface StockManager {

    int reserve(UUID productId, int quantity, UUID operationId);

    int release(UUID productId, int quantity, UUID operationId);
}
