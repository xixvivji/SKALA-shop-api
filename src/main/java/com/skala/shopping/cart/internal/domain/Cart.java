package com.skala.shopping.cart.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "cart")
public class Cart {
    @Id
    @Column(name = "member_id")
    private UUID memberId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cart() { }
    public Cart(UUID memberId, Instant now) {
        this.memberId = memberId;
        this.createdAt = now;
        this.updatedAt = now;
    }
    public void touch(Instant now) { this.updatedAt = now; }
}
