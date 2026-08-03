package com.skala.shopping.member.internal.domain;

import com.skala.shopping.member.MemberResponse;
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
@Table(name = "members", schema = "member")
public class Member {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 100)
    private String customerId;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Member() {
    }

    public Member(UUID id, String customerId, String name, Instant now) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateName(String name, Instant now) {
        this.name = name;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.status = MemberStatus.INACTIVE;
        this.updatedAt = now;
    }

    public MemberResponse toResponse() {
        return new MemberResponse(id, customerId, name, status.name());
    }
}
