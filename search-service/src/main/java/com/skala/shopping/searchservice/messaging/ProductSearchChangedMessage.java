package com.skala.shopping.searchservice.messaging;

import java.math.BigDecimal;
import java.util.UUID;

/** Backend의 ProductSearchChanged JSON 계약을 RDS나 Backend 코드 의존 없이 소비합니다. */
public final class ProductSearchChangedMessage {

    private UUID id;
    private String name;
    private BigDecimal price;
    private UUID categoryId;
    private String description;
    private String imageUrl;
    private boolean deleted;

    public ProductSearchChangedMessage() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
