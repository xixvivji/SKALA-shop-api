package com.skala.shopping.storefront.internal.web.dto.request;

import com.skala.shopping.auth.BcryptCompatible;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ResetPasswordRequest",
        description = "고객 ID와 현재 등록된 이름을 확인하는 데모용 비밀번호 재설정 요청"
)
public final class ResetPasswordRequest {

    @Schema(description = "로그인에 사용하는 고객 ID", example = "skala01")
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "[A-Za-z0-9_-]+")
    private String customerId;

    @Schema(description = "현재 등록된 고객 이름", example = "김스칼라")
    @NotBlank
    @Size(max = 100)
    private String customerName;

    @Schema(description = "새 비밀번호", example = "newPassword123")
    @NotBlank
    @Size(min = 6, max = 72)
    @BcryptCompatible
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String customerId, String customerName, String newPassword) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.newPassword = newPassword;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
