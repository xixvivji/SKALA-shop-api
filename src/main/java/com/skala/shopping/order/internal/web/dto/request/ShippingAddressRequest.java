package com.skala.shopping.order.internal.web.dto.request;

import com.skala.shopping.order.ShippingAddressCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ShippingAddressRequest {
    @NotBlank @Size(max=100) private String recipientName;
    @NotBlank @Size(max=30) @Pattern(regexp="[0-9+() -]{7,30}") private String phoneNumber;
    @NotBlank @Size(max=20) private String postalCode;
    @NotBlank @Size(max=300) private String addressLine1;
    @Size(max=300) private String addressLine2;
    public ShippingAddressRequest() { }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { recipientName=value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber=value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode=value; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String value) { addressLine1=value; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String value) { addressLine2=value; }
    public ShippingAddressCommand toCommand() {
        return new ShippingAddressCommand(recipientName, phoneNumber, postalCode, addressLine1, addressLine2);
    }
}
