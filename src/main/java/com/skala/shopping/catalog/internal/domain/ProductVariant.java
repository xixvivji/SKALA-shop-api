package com.skala.shopping.catalog.internal.domain;

import com.skala.shopping.catalog.ProductVariantSnapshot;
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
@Table(name = "product_variants", schema = "catalog")
public class ProductVariant {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false, length = 100, unique = true) private String sku;
    @Column(name = "option_name", length = 50) private String optionName;
    @Column(name = "option_value", length = 100) private String optionValue;
    @Column(name = "additional_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal additionalPrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProductStatus status;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ProductVariant() { }

    public ProductVariant(UUID id, UUID productId, String sku, String optionName,
                          String optionValue, BigDecimal additionalPrice, Instant now) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.additionalPrice = additionalPrice;
        this.status = ProductStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() { return id; }
    public UUID productId() { return productId; }
    public boolean isSaleable() { return status == ProductStatus.ACTIVE; }
    public void deactivate(Instant now) { status = ProductStatus.DELETED; updatedAt = now; }
    public ProductVariantSnapshot toSnapshot(ProductSnapshotSource product) {
        return new ProductVariantSnapshot(id, product.id(), product.name(), sku, optionName,
                optionValue, product.price().add(additionalPrice), status.name());
    }

    public static final class ProductSnapshotSource {
        private final UUID id; private final String name; private final BigDecimal price;
        public ProductSnapshotSource(UUID id, String name, BigDecimal price) {
            this.id = id; this.name = name; this.price = price;
        }
        public UUID id() { return id; }
        public String name() { return name; }
        public BigDecimal price() { return price; }
    }
}
