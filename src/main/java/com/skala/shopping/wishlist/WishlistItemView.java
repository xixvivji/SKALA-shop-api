package com.skala.shopping.wishlist;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WishlistItemView {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final BigDecimal productPrice;
    private final Instant addedAt;

    public WishlistItemView(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal productPrice,
            Instant addedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.addedAt = addedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
