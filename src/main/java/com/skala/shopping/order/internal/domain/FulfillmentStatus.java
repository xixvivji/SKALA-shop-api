package com.skala.shopping.order.internal.domain;

public enum FulfillmentStatus {
    PAID, PREPARING, SHIPPED, DELIVERED;

    public boolean canTransitionTo(FulfillmentStatus next) {
        return next != null && next.ordinal() == ordinal() + 1;
    }

    public boolean isCancelable() { return this == PAID || this == PREPARING; }
}
