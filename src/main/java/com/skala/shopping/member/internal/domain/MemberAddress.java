package com.skala.shopping.member.internal.domain;

import com.skala.shopping.member.MemberAddressView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "member_addresses",
        schema = "member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_addresses_member_name",
                columnNames = {"member_id", "address_name"}
        )
)
public class MemberAddress {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "address_name", nullable = false, length = 50)
    private String addressName;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "address_line1", nullable = false, length = 300)
    private String addressLine1;

    @Column(name = "address_line2", length = 300)
    private String addressLine2;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemberAddress() {
    }

    public MemberAddress(
            UUID memberId,
            String addressName,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String addressLine1,
            String addressLine2,
            boolean defaultAddress,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        update(
                addressName,
                recipientName,
                phoneNumber,
                postalCode,
                addressLine1,
                addressLine2,
                defaultAddress,
                now
        );
        this.createdAt = now;
    }

    public void update(
            String addressName,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String addressLine1,
            String addressLine2,
            boolean defaultAddress,
            Instant now
    ) {
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.postalCode = postalCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.defaultAddress = defaultAddress;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void makeDefault(Instant now) {
        defaultAddress = true;
        updatedAt = now;
    }

    public MemberAddressView toView() {
        return new MemberAddressView(
                id,
                addressName,
                recipientName,
                phoneNumber,
                postalCode,
                addressLine1,
                addressLine2,
                defaultAddress
        );
    }
}
