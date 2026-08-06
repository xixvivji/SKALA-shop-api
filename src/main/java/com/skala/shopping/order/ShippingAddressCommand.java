package com.skala.shopping.order;

public final class ShippingAddressCommand {
    private final String recipientName;
    private final String phoneNumber;
    private final String postalCode;
    private final String addressLine1;
    private final String addressLine2;
    public ShippingAddressCommand(String recipientName, String phoneNumber, String postalCode,
                                  String addressLine1, String addressLine2) {
        this.recipientName = recipientName; this.phoneNumber = phoneNumber; this.postalCode = postalCode;
        this.addressLine1 = addressLine1; this.addressLine2 = addressLine2;
    }
    public String getRecipientName() { return recipientName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPostalCode() { return postalCode; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
}
