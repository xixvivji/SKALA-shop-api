package com.skala.shopping.searchservice.api;

import com.skala.shopping.searchservice.domain.ProductSearchDocument;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductSearchResponse {

    private final UUID id;
    private final String name;
    private final BigDecimal price;
    private final UUID categoryId;
    private final String description;
    private final String imageUrl;
    private final String status;

    public ProductSearchResponse(ProductSearchDocument document) {
        this.id = UUID.fromString(document.getId());
        this.name = document.getName();
        this.price = document.getPrice();
        this.categoryId = document.getCategoryId() == null
                ? null
                : UUID.fromString(document.getCategoryId());
        this.description = document.getDescription();
        this.imageUrl = document.getImageUrl();
        this.status = "ACTIVE";
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }
}
