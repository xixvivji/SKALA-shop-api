package com.skala.shopping.order.internal.domain;

public enum OrderStatus {
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    PAID,
    PARTIALLY_CANCELED,
    CANCELED
}
