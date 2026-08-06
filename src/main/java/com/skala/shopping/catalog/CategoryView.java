package com.skala.shopping.catalog;

import java.util.UUID;

public final class CategoryView {
    private final UUID id; private final String name; private final String description; private final String status;
    public CategoryView(UUID id, String name, String description, String status) {
        this.id=id; this.name=name; this.description=description; this.status=status;
    }
    public UUID getId(){return id;} public String getName(){return name;}
    public String getDescription(){return description;} public String getStatus(){return status;}
}
