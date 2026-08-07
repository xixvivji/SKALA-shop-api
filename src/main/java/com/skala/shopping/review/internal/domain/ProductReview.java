package com.skala.shopping.review.internal.domain;

import com.skala.shopping.review.ReviewResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_reviews", schema = "reviews")
public class ProductReview {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ProductReview() {
    }

    public ProductReview(UUID memberId, UUID productId, int rating, String comment, Instant now) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = now;
        this.updatedAt = now;
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

    public int rating() {
        return rating;
    }

    public String comment() {
        return comment;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void update(int rating, String comment, Instant now) {
        this.rating = rating;
        this.comment = comment;
        this.updatedAt = now;
    }

    public ReviewResponse toResponse() {
        return new ReviewResponse(
                id,
                productId,
                rating,
                comment,
                createdAt,
                updatedAt
        );
    }
}
