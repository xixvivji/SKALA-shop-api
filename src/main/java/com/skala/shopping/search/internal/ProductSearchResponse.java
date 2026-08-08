package com.skala.shopping.search.internal;

import java.math.BigDecimal; import java.util.UUID;
import com.skala.shopping.catalog.ProductSnapshot;

public final class ProductSearchResponse {
    private final UUID id; private final String name; private final BigDecimal price;
    private final UUID categoryId; private final String description; private final String imageUrl;
    ProductSearchResponse(ProductSearchDocument document){id=UUID.fromString(document.id());name=document.name();price=document.price();
        categoryId=document.categoryId()==null?null:UUID.fromString(document.categoryId());description=document.description();imageUrl=document.imageUrl();}
    ProductSearchResponse(ProductSnapshot product){id=product.getId();name=product.getName();price=product.getPrice();
        categoryId=product.getCategoryId();description=product.getDescription();imageUrl=product.getImageUrl();}
    public UUID getId(){return id;} public String getName(){return name;} public BigDecimal getPrice(){return price;}
    public UUID getCategoryId(){return categoryId;} public String getDescription(){return description;} public String getImageUrl(){return imageUrl;}
    public String getStatus(){return "ACTIVE";}
}
