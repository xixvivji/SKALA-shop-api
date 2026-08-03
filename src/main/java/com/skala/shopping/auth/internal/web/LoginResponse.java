package com.skala.shopping.auth.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "LoginResponse", description = "로그인 결과")
final class LoginResponse {

    private final UUID memberId;
    private final String customerId;
    private final String role;
    private final Instant expiresAt;

    LoginResponse(UUID memberId, String customerId, String role, Instant expiresAt) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRole() {
        return role;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
