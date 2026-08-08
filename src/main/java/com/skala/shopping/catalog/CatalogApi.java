package com.skala.shopping.catalog;

import java.util.UUID;
import java.util.List;

public interface CatalogApi {

    ProductSnapshot getSaleableProduct(UUID productId);

    ProductVariantSnapshot getSaleableVariant(UUID productId, UUID variantId);

    default List<ProductSnapshot> getSaleableProducts(List<UUID> productIds) {
        return productIds.stream().map(this::getSaleableProduct).toList();
    }
}
