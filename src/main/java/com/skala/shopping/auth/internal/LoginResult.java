package com.skala.shopping.auth.internal;

import java.time.Instant;
import java.util.UUID;

public final class LoginResult {

    private final UUID memberId;
    private final String loginId;
    private final String role;
    private final String accessToken;
    private final Instant expiresAt;

    public LoginResult(
            UUID memberId,
            String loginId,
            String role,
            String accessToken,
            Instant expiresAt
    ) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.role = role;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getRole() {
        return role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
