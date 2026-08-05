package com.skala.shopping.auth.internal.web;

import com.skala.shopping.auth.BcryptCompatible;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ChangeAdminPasswordRequest", description = "관리자 비밀번호 변경 요청")
final class ChangeAdminPasswordRequest {

    @Schema(description = "현재 관리자 비밀번호")
    @NotBlank
    @Size(max = 72)
    @BcryptCompatible
    private String currentPassword;

    @Schema(description = "새 관리자 비밀번호(12자 이상)")
    @NotBlank
    @Size(min = 12, max = 72)
    @BcryptCompatible
    private String newPassword;

    public ChangeAdminPasswordRequest() {
    }

    public ChangeAdminPasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
