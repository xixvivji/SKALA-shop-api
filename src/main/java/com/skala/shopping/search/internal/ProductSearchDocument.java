package com.skala.shopping.search.internal;

import com.skala.shopping.catalog.ProductSearchChanged; import java.math.BigDecimal; import java.util.UUID;
import org.springframework.data.annotation.Id; import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field; import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName="skala-products",createIndex=false)
class ProductSearchDocument {
    @Id private String id;
    @Field(type=FieldType.Text) private String name;
    @Field(type=FieldType.Double) private BigDecimal price;
    @Field(type=FieldType.Keyword) private String categoryId;
    @Field(type=FieldType.Text) private String description;
    @Field(type=FieldType.Keyword,index=false) private String imageUrl;
    protected ProductSearchDocument(){}
    ProductSearchDocument(ProductSearchChanged event){id=event.getId().toString();name=event.getName();price=event.getPrice();
        categoryId=event.getCategoryId()==null?null:event.getCategoryId().toString();description=event.getDescription();imageUrl=event.getImageUrl();}
    String id(){return id;} String name(){return name;} BigDecimal price(){return price;}
    String categoryId(){return categoryId;} String description(){return description;} String imageUrl(){return imageUrl;}
}
