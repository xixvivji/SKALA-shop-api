package com.skala.shopping.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductSnapshot {

    private final UUID id;
    private final String name;
    private final BigDecimal price;
    private final String status;
    private final UUID categoryId;
    private final String description;
    private final String imageUrl;

    public ProductSnapshot(UUID id, String name, BigDecimal price, String status) {
        this(id, name, price, status, null, null, null);
    }

    public ProductSnapshot(UUID id, String name, BigDecimal price, String status,
                           UUID categoryId, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
        this.categoryId = categoryId;
        this.description = description;
        this.imageUrl = imageUrl;
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
    public UUID getCategoryId() { return categoryId; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
}
