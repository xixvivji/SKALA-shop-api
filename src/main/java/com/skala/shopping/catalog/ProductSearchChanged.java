package com.skala.shopping.catalog;

import java.math.BigDecimal; import java.util.UUID;

/** 검색 색인처럼 카탈로그 외부 읽기 모델이 소비하는 상품 변경 이벤트입니다. */
public final class ProductSearchChanged {
    private final UUID id; private final String name; private final BigDecimal price; private final UUID categoryId;
    private final String description; private final String imageUrl; private final boolean deleted;
    public ProductSearchChanged(ProductSnapshot product,boolean deleted){this.id=product.getId();this.name=product.getName();
        this.price=product.getPrice();this.categoryId=product.getCategoryId();this.description=product.getDescription();
        this.imageUrl=product.getImageUrl();this.deleted=deleted;}
    public UUID getId(){return id;} public String getName(){return name;} public BigDecimal getPrice(){return price;}
    public UUID getCategoryId(){return categoryId;} public String getDescription(){return description;}
    public String getImageUrl(){return imageUrl;} public boolean isDeleted(){return deleted;}
}
