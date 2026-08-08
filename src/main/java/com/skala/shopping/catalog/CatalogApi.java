package com.skala.shopping.catalog;

import java.util.UUID;
import java.util.List;
import com.skala.shopping.common.PageResponse;
import java.math.BigDecimal;

public interface CatalogApi {

    ProductSnapshot getSaleableProduct(UUID productId);

    ProductVariantSnapshot getSaleableVariant(UUID productId, UUID variantId);

    PageResponse<ProductSnapshot> getProducts(int page, int size);

    PageResponse<ProductSnapshot> searchProducts(String query, UUID categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, int page, int size);

    default List<ProductSnapshot> getSaleableProducts(List<UUID> productIds) {
        return productIds.stream().map(this::getSaleableProduct).toList();
    }
}
