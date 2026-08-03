package com.skala.shopping.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductSnapshot {

    private final UUID id;
    private final String name;
    private final BigDecimal price;
    private final String status;

    public ProductSnapshot(UUID id, String name, BigDecimal price, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}
