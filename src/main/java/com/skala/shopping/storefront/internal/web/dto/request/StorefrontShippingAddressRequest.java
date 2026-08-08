package com.skala.shopping.storefront.internal.web.dto.request;

import com.skala.shopping.order.ShippingAddressCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "StorefrontShippingAddressRequest", description = "호환 주문 배송지 요청")
public final class StorefrontShippingAddressRequest {

    @Schema(description = "수령인 이름", example = "김스칼라")
    @NotBlank
    @Size(max = 100)
    private String recipientName;

    @Schema(description = "수령인 연락처", example = "010-1234-5678")
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "[0-9+() -]{7,30}")
    private String phoneNumber;

    @Schema(description = "우편번호", example = "04524")
    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @Schema(description = "기본 주소", example = "서울특별시 중구 세종대로 110")
    @NotBlank
    @Size(max = 300)
    private String addressLine1;

    @Schema(description = "상세 주소", example = "10층")
    @Size(max = 300)
    private String addressLine2;

    public StorefrontShippingAddressRequest() {
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public ShippingAddressCommand toCommand() {
        return new ShippingAddressCommand(
                recipientName,
                phoneNumber,
                postalCode,
                addressLine1,
                addressLine2
        );
    }
}
