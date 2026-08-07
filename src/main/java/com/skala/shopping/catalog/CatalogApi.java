package com.skala.shopping.catalog;

import java.util.UUID;
import java.util.List;

public interface CatalogApi {

    ProductSnapshot getSaleableProduct(UUID productId);

    default List<ProductSnapshot> getSaleableProducts(List<UUID> productIds) {
        return productIds.stream().map(this::getSaleableProduct).toList();
    }
}
