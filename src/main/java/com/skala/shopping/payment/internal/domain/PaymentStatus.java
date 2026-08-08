package com.skala.shopping.payment.internal.domain;

public enum PaymentStatus {
    READY,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
