package com.skala.shopping.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentView {
    private final UUID id;
    private final UUID orderId;
    private final String provider;
    private final String providerTransactionId;
    private final String method;
    private final String maskedNumber;
    private final BigDecimal requestedAmount;
    private final BigDecimal approvedAmount;
    private final BigDecimal refundedAmount;
    private final String status;
    private final String failureCode;
    private final String failureMessage;
    private final Instant approvedAt;
    private final Instant createdAt;

    public PaymentView(UUID id, UUID orderId, String provider, String providerTransactionId,
                       String method, String maskedNumber, BigDecimal requestedAmount,
                       BigDecimal approvedAmount, BigDecimal refundedAmount, String status,
                       String failureCode, String failureMessage, Instant approvedAt,
                       Instant createdAt) {
        this.id = id; this.orderId = orderId; this.provider = provider;
        this.providerTransactionId = providerTransactionId; this.method = method;
        this.maskedNumber = maskedNumber; this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount; this.refundedAmount = refundedAmount;
        this.status = status; this.failureCode = failureCode;
        this.failureMessage = failureMessage; this.approvedAt = approvedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getProvider() { return provider; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getMethod() { return method; }
    public String getMaskedNumber() { return maskedNumber; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public String getStatus() { return status; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
