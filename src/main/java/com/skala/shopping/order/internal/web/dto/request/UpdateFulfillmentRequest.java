package com.skala.shopping.order.internal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class UpdateFulfillmentRequest {
    @NotBlank
    @Pattern(regexp="PREPARING|SHIPPED|DELIVERED")
    private String status;
    public UpdateFulfillmentRequest() { }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
