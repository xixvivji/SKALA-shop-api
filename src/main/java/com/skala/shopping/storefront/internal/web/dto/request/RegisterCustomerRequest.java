package com.skala.shopping.storefront.internal.web.dto.request;

import com.skala.shopping.auth.BcryptCompatible;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterCustomerRequest", description = "회원가입 요청")
public final class RegisterCustomerRequest {

    @Schema(description = "로그인에 사용할 고객 ID", example = "skala01")
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "[A-Za-z0-9_-]+")
    private String customerId;

    @Schema(description = "비밀번호", example = "pw1234")
    @NotBlank
    @Size(min = 6, max = 72)
    @BcryptCompatible
    private String customerPassword;

    @Schema(description = "고객 이름", example = "김스칼라")
    @NotBlank
    @Size(max = 100)
    private String customerName;

    public RegisterCustomerRequest() {
    }

    public RegisterCustomerRequest(String customerId, String customerPassword, String customerName) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        this.customerName = customerName;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
