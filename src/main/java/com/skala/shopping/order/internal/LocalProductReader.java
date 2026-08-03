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
    public OrderProduct getSaleableProduct(UUID productId) {
        var product = catalogApi.getSaleableProduct(productId);
        return new OrderProduct(product.getId(), product.getName(), product.getPrice());
    }
}
