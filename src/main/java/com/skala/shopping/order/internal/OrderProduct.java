package com.skala.shopping.order.internal;

import java.math.BigDecimal;
import java.util.UUID;

final class OrderProduct {

    private final UUID id;
    private final UUID variantId;
    private final String sku;
    private final String optionName;
    private final String optionValue;
    private final String name;
    private final BigDecimal price;

    OrderProduct(UUID id, String name, BigDecimal price) {
        this(id, id, null, null, null, name, price);
    }

    OrderProduct(UUID id, UUID variantId, String sku, String optionName, String optionValue,
                 String name, BigDecimal price) {
        this.id = id;
        this.variantId = variantId;
        this.sku = sku;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.name = name;
        this.price = price;
    }

    UUID getVariantId() { return variantId; }
    String getSku() { return sku; }
    String getOptionName() { return optionName; }
    String getOptionValue() { return optionValue; }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    BigDecimal getPrice() {
        return price;
    }
}
