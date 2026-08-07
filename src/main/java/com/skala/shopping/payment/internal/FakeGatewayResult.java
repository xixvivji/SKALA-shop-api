package com.skala.shopping.payment.internal;

final class FakeGatewayResult {
    private final boolean approved;
    private final String transactionId;
    private final String failureCode;
    private final String failureMessage;

    FakeGatewayResult(boolean approved, String transactionId, String failureCode, String failureMessage) {
        this.approved = approved; this.transactionId = transactionId;
        this.failureCode = failureCode; this.failureMessage = failureMessage;
    }
    boolean approved() { return approved; }
    String transactionId() { return transactionId; }
    String failureCode() { return failureCode; }
    String failureMessage() { return failureMessage; }
}
