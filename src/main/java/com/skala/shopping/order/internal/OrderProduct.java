package com.skala.shopping.order.internal;

import java.math.BigDecimal;
import java.util.UUID;

final class OrderProduct {

    private final UUID id;
    private final String name;
    private final BigDecimal price;

    OrderProduct(UUID id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

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
