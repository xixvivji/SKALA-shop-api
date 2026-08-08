package com.skala.shopping.cart.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "cart")
public class CartItem {
    @Id
    private UUID id;
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "variant_id", nullable = false)
    private UUID variantId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() { }
    public CartItem(UUID memberId, UUID productId, UUID variantId, int quantity, Instant now) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.createdAt = now;
        this.updatedAt = now;
    }
    public void changeQuantity(int quantity, Instant now) { this.quantity = quantity; this.updatedAt = now; }
    public UUID getProductId() { return productId; }
    public UUID getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
}
