package com.skala.shopping.auth.internal.web;

import jakarta.validation.constraints.NotBlank;

final class LoginRequest {

    @NotBlank
    private String customerId;

    @NotBlank
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
