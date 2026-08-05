package com.skala.shopping.member;

import java.util.UUID;

public final class MemberAddressView {

    private final UUID id;
    private final String addressName;
    private final String recipientName;
    private final String phoneNumber;
    private final String postalCode;
    private final String addressLine1;
    private final String addressLine2;
    private final boolean defaultAddress;

    public MemberAddressView(
            UUID id,
            String addressName,
            String recipientName,
            String phoneNumber,
            String postalCode,
            String addressLine1,
            String addressLine2,
            boolean defaultAddress
    ) {
        this.id = id;
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.postalCode = postalCode;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.defaultAddress = defaultAddress;
    }

    public UUID getId() {
        return id;
    }

    public String getAddressName() {
        return addressName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
}
