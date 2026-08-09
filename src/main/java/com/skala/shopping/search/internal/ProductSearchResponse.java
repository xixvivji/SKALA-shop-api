package com.skala.shopping.search.internal;

import com.skala.shopping.catalog.ProductSnapshot;
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

    ProductSearchResponse(SearchServiceClient.SearchProduct product) {
        this(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategoryId(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus()
        );
    }

    ProductSearchResponse(ProductSnapshot product) {
        this(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategoryId(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus()
        );
    }

    private ProductSearchResponse(
            UUID id,
            String name,
            BigDecimal price,
            UUID categoryId,
            String description,
            String imageUrl,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.categoryId = categoryId;
        this.description = description;
        this.imageUrl = imageUrl;
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
