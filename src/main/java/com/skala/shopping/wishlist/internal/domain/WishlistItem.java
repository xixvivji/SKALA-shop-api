package com.skala.shopping.wishlist.internal.domain;

import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.wishlist.WishlistItemView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items", schema = "wishlist")
public class WishlistItem {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected WishlistItem() {
    }

    public WishlistItem(UUID memberId, UUID productId, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID memberId() {
        return memberId;
    }

    public UUID productId() {
        return productId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public WishlistItemView toView(ProductSnapshot product) {
        return new WishlistItemView(
                id,
                productId,
                product != null ? product.getName() : "알 수 없는 상품",
                product != null ? product.getPrice() : null,
                createdAt
        );
    }
}
