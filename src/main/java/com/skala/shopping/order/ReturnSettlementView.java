package com.skala.shopping.order;

import java.math.BigDecimal;

public final class ReturnSettlementView {
    private final BigDecimal refundAmount;
    private final BigDecimal pointRefundAmount;
    private final BigDecimal balanceAfter;
    public ReturnSettlementView(BigDecimal refundAmount, BigDecimal pointRefundAmount,
                                BigDecimal balanceAfter) {
        this.refundAmount = refundAmount; this.pointRefundAmount = pointRefundAmount;
        this.balanceAfter = balanceAfter;
    }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public BigDecimal getPointRefundAmount() { return pointRefundAmount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
}
