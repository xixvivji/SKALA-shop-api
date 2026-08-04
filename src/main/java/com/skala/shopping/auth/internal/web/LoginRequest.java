package com.skala.shopping.auth.internal.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "고객 로그인 요청")
final class LoginRequest {

    @Schema(description = "고객 ID", example = "skala01")
    @NotBlank
    @Size(max = 50)
    private String customerId;

    @Schema(description = "고객 비밀번호")
    @NotBlank
    @Size(max = 72)
    private String customerPassword;

    public LoginRequest() {
    }

    public LoginRequest(String customerId, String customerPassword) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerPassword() {
        return customerPassword;
    }

    public void setCustomerPassword(String customerPassword) {
        this.customerPassword = customerPassword;
    }
}
