package com.skala.shopping.order.internal.web.dto.request;

import com.skala.shopping.order.ShippingAddressCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "ShippingAddressRequest", description = "주문 배송지 요청")
public final class ShippingAddressRequest {
    @Schema(description = "수령인 이름", example = "김스칼라")
    @NotBlank @Size(max=100) private String recipientName;
    @Schema(description = "수령인 연락처", example = "010-1234-5678")
    @NotBlank @Size(max=30) @Pattern(regexp="[0-9+() -]{7,30}") private String phoneNumber;
    @Schema(description = "우편번호", example = "04524")
    @NotBlank @Size(max=20) private String postalCode;
    @Schema(description = "기본 주소", example = "서울특별시 중구 세종대로 110")
    @NotBlank @Size(max=300) private String addressLine1;
    @Schema(description = "상세 주소", example = "10층")
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
