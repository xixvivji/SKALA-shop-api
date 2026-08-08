package com.skala.shopping.payment.internal.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class RefundPaymentRequest {
    @NotNull @DecimalMin("0.01") @Digits(integer = 14, fraction = 2)
    private BigDecimal amount;
    public RefundPaymentRequest() { }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
