package com.skala.shopping.searchservice.domain;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "skala-products", createIndex = false)
public class ProductSearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword, index = false)
    private String imageUrl;

    protected ProductSearchDocument() {
    }

    public ProductSearchDocument(
            UUID id,
            String name,
            BigDecimal price,
            UUID categoryId,
            String description,
            String imageUrl
    ) {
        this.id = id.toString();
        this.name = name;
        this.price = price;
        this.categoryId = categoryId == null ? null : categoryId.toString();
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
