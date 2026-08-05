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
        this(id, loginId, passwordHash, AccountRole.CUSTOMER, now);
    }

    private AuthAccount(
            UUID id,
            String loginId,
            String passwordHash,
            AccountRole role,
            Instant now
    ) {
        this.id = id;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AuthAccount createAdmin(
            UUID id,
            String loginId,
            String passwordHash,
            Instant now
    ) {
        return new AuthAccount(id, loginId, passwordHash, AccountRole.ADMIN, now);
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

    public long credentialVersion() {
        return version;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == AccountRole.ADMIN;
    }

    public void deactivate(Instant now) {
        status = AccountStatus.INACTIVE;
        updatedAt = now;
    }

    public void changePassword(String newPasswordHash, Instant now) {
        passwordHash = newPasswordHash;
        updatedAt = now;
    }
}
