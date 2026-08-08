package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

public final class ReturnableOrderItemView {
    private final UUID orderId;
    private final UUID orderItemId;
    private final UUID memberId;
    private final UUID productId;
    private final String productName;
    private final int returnableQuantity;
    private final BigDecimal refundableAmount;
    private final BigDecimal pointRatio;

    public ReturnableOrderItemView(UUID orderId, UUID orderItemId, UUID memberId,
                                   UUID productId, String productName, int returnableQuantity,
                                   BigDecimal refundableAmount, BigDecimal pointRatio) {
        this.orderId = orderId; this.orderItemId = orderItemId; this.memberId = memberId;
        this.productId = productId; this.productName = productName;
        this.returnableQuantity = returnableQuantity; this.refundableAmount = refundableAmount;
        this.pointRatio = pointRatio;
    }
    public UUID getOrderId() { return orderId; }
    public UUID getOrderItemId() { return orderItemId; }
    public UUID getMemberId() { return memberId; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getReturnableQuantity() { return returnableQuantity; }
    public BigDecimal getRefundableAmount() { return refundableAmount; }
    public BigDecimal getPointRatio() { return pointRatio; }
}
