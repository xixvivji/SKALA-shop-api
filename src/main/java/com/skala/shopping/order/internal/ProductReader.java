package com.skala.shopping.order.internal;

import java.util.UUID;

interface ProductReader {

    OrderProduct getSaleableProduct(UUID productId);
}
