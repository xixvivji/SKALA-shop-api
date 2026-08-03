package com.skala.shopping.auth.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "auth")
public class AuthAccount {

    @Id
    private UUID id;

    @Column(name = "login_id", nullable = false, unique = true, length = 100)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AuthAccount() {
    }

    public AuthAccount(UUID id, String loginId, String passwordHash, Instant now) {
        this.id = id;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.role = AccountRole.CUSTOMER;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String loginId() {
        return loginId;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public AccountRole role() {
        return role;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void deactivate(Instant now) {
        status = AccountStatus.INACTIVE;
        updatedAt = now;
    }
}
