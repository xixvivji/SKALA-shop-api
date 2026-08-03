package com.skala.shopping.auth.internal.web;

import java.time.Instant;
import java.util.UUID;

final class LoginResponse {

    private final UUID memberId;
    private final String customerId;
    private final Instant expiresAt;

    LoginResponse(UUID memberId, String customerId, Instant expiresAt) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.expiresAt = expiresAt;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
