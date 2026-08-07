package com.skala.shopping.review;

import java.time.Instant;
import java.util.UUID;

public final class ReviewResponse {

    private final UUID id;
    private final UUID productId;
    private final UUID memberId;
    private final int rating;
    private final String comment;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ReviewResponse(
            UUID id,
            UUID productId,
            UUID memberId,
            int rating,
            String comment,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.memberId = memberId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
