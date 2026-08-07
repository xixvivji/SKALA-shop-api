package com.skala.shopping.payment.internal.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class PreparePaymentRequest {
    @NotNull private UUID orderId;
    private String method = "CARD";
    public PreparePaymentRequest() { }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
