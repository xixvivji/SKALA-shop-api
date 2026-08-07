package com.skala.shopping.order.internal.domain;

public enum FulfillmentStatus {
    PAYMENT_PENDING, PAID, PREPARING, SHIPPED, DELIVERED;

    public boolean canTransitionTo(FulfillmentStatus next) {
        return next != null && next.ordinal() == ordinal() + 1;
    }

    public boolean isCancelable() { return this == PAID || this == PREPARING; }
}
