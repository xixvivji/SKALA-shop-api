package com.skala.shopping.storefront.internal.web;

import com.skala.shopping.order.OrderView;
import java.math.BigDecimal;

final class OrderCompatibilityResponse {

    private final OrderView order;
    private final BigDecimal remainingPoints;

    OrderCompatibilityResponse(OrderView order, BigDecimal remainingPoints) {
        this.order = order;
        this.remainingPoints = remainingPoints;
    }

    public OrderView getOrder() {
        return order;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }
}
