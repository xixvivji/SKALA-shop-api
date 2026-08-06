package com.skala.shopping.catalog.internal.domain;

import com.skala.shopping.catalog.CategoryView;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="categories", schema="catalog")
public class Category {
    @Id private UUID id;
    @Column(nullable=false,length=100) private String name;
    @Column(length=500) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ProductStatus status;
    @Version private long version;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected Category(){}
    public Category(String name,String description,Instant now){id=UUID.randomUUID();this.name=name;
        this.description=description;status=ProductStatus.ACTIVE;createdAt=now;updatedAt=now;}
    public void update(String name,String description,Instant now){this.name=name;this.description=description;updatedAt=now;}
    public void delete(Instant now){status=ProductStatus.DELETED;updatedAt=now;}
    public CategoryView toView(){return new CategoryView(id,name,description,status.name());}
}
