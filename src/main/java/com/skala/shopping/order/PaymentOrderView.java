package com.skala.shopping.order;

import java.math.BigDecimal;
import java.util.UUID;

/** 결제 모듈에 노출하는 최소 주문 계약으로, 주문 엔티티를 모듈 밖에 공개하지 않습니다. */
public final class PaymentOrderView {
    private final UUID orderId;
    private final UUID memberId;
    private final BigDecimal paymentAmount;
    private final String status;

    public PaymentOrderView(UUID orderId, UUID memberId, BigDecimal paymentAmount, String status) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.paymentAmount = paymentAmount;
        this.status = status;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getMemberId() { return memberId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public String getStatus() { return status; }
}
