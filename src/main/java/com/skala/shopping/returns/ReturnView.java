package com.skala.shopping.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ReturnView {
    private final UUID id; private final UUID orderId; private final UUID orderItemId;
    private final UUID productId; private final String productName; private final int quantity;
    private final String reason; private final String evidenceImageUrl; private final String status;
    private final BigDecimal grossRefundAmount; private final BigDecimal shippingFee;
    private final BigDecimal refundAmount; private final BigDecimal pointRefundAmount;
    private final BigDecimal paymentRefundAmount; private final BigDecimal balanceAfter;
    private final String adminNote; private final Instant requestedAt; private final Instant updatedAt;

    public ReturnView(UUID id, UUID orderId, UUID orderItemId, UUID productId,
                      String productName, int quantity, String reason, String evidenceImageUrl,
                      String status, BigDecimal grossRefundAmount, BigDecimal shippingFee,
                      BigDecimal refundAmount, BigDecimal pointRefundAmount,
                      BigDecimal paymentRefundAmount, BigDecimal balanceAfter,
                      String adminNote, Instant requestedAt, Instant updatedAt) {
        this.id=id; this.orderId=orderId; this.orderItemId=orderItemId;
        this.productId=productId; this.productName=productName; this.quantity=quantity;
        this.reason=reason; this.evidenceImageUrl=evidenceImageUrl; this.status=status;
        this.grossRefundAmount=grossRefundAmount; this.shippingFee=shippingFee;
        this.refundAmount=refundAmount; this.pointRefundAmount=pointRefundAmount;
        this.paymentRefundAmount=paymentRefundAmount; this.balanceAfter=balanceAfter;
        this.adminNote=adminNote; this.requestedAt=requestedAt; this.updatedAt=updatedAt;
    }
    public UUID getId(){return id;} public UUID getOrderId(){return orderId;}
    public UUID getOrderItemId(){return orderItemId;} public UUID getProductId(){return productId;}
    public String getProductName(){return productName;} public int getQuantity(){return quantity;}
    public String getReason(){return reason;} public String getEvidenceImageUrl(){return evidenceImageUrl;}
    public String getStatus(){return status;} public BigDecimal getGrossRefundAmount(){return grossRefundAmount;}
    public BigDecimal getShippingFee(){return shippingFee;} public BigDecimal getRefundAmount(){return refundAmount;}
    public BigDecimal getPointRefundAmount(){return pointRefundAmount;}
    public BigDecimal getPaymentRefundAmount(){return paymentRefundAmount;}
    public BigDecimal getBalanceAfter(){return balanceAfter;} public String getAdminNote(){return adminNote;}
    public Instant getRequestedAt(){return requestedAt;} public Instant getUpdatedAt(){return updatedAt;}
}
