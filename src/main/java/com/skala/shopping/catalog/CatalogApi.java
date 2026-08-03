package com.skala.shopping.catalog;

import java.util.UUID;

public interface CatalogApi {

    ProductSnapshot getSaleableProduct(UUID productId);
}
