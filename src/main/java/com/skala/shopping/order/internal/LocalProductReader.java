package com.skala.shopping.order.internal;

import com.skala.shopping.catalog.CatalogApi;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalProductReader implements ProductReader {

    private final CatalogApi catalogApi;

    LocalProductReader(CatalogApi catalogApi) {
        this.catalogApi = catalogApi;
    }

    @Override
    public OrderProduct getSaleableProduct(UUID productId, UUID variantId) {
        var product = catalogApi.getSaleableVariant(productId, variantId);
        return new OrderProduct(product.getProductId(), product.getId(), product.getSku(),
                product.getOptionName(), product.getOptionValue(), product.getDisplayName(), product.getPrice());
    }
}
