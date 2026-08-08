package com.skala.shopping.returns.internal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UpdateReturnStatusRequest {
    @NotBlank private String status;
    @Size(max=500) private String adminNote;
    public UpdateReturnStatusRequest(){}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public String getAdminNote(){return adminNote;} public void setAdminNote(String value){adminNote=value;}
}
