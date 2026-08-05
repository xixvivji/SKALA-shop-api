package com.skala.shopping.member.internal.web.dto.response;

import com.skala.shopping.member.MemberAddressView;
import java.util.UUID;

public final class MemberAddressResponse {

    private final UUID id;
    private final String addressName;
    private final String recipientName;
    private final String phoneNumber;
    private final String postalCode;
    private final String addressLine1;
    private final String addressLine2;
    private final boolean defaultAddress;

    private MemberAddressResponse(MemberAddressView address) {
        this.id = address.getId();
        this.addressName = address.getAddressName();
        this.recipientName = address.getRecipientName();
        this.phoneNumber = address.getPhoneNumber();
        this.postalCode = address.getPostalCode();
        this.addressLine1 = address.getAddressLine1();
        this.addressLine2 = address.getAddressLine2();
        this.defaultAddress = address.isDefaultAddress();
    }

    public static MemberAddressResponse from(MemberAddressView address) {
        return new MemberAddressResponse(address);
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
