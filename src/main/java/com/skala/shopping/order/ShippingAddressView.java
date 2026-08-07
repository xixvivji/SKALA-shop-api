package com.skala.shopping.order;

public final class ShippingAddressView {
    private final String recipientName, phoneNumber, postalCode, addressLine1, addressLine2;
    public ShippingAddressView(String recipientName,String phoneNumber,String postalCode,String addressLine1,String addressLine2){
        this.recipientName=recipientName;this.phoneNumber=phoneNumber;this.postalCode=postalCode;
        this.addressLine1=addressLine1;this.addressLine2=addressLine2;}
    public String getRecipientName(){return recipientName;} public String getPhoneNumber(){return phoneNumber;}
    public String getPostalCode(){return postalCode;} public String getAddressLine1(){return addressLine1;}
    public String getAddressLine2(){return addressLine2;}
}
