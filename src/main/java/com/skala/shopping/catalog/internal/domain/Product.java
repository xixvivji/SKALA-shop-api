package com.skala.shopping.catalog.internal.domain;

import com.skala.shopping.catalog.ProductSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "catalog")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String name, BigDecimal price, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.price = price;
        this.status = ProductStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, BigDecimal price, Instant now) {
        this.name = name;
        this.price = price;
        this.updatedAt = now;
    }

    public void delete(Instant now) {
        this.status = ProductStatus.DELETED;
        this.updatedAt = now;
    }

    public boolean isSaleable() {
        return status == ProductStatus.ACTIVE;
    }

    public ProductSnapshot toSnapshot() {
        return new ProductSnapshot(id, name, price, status.name());
    }
}
