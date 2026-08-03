package com.skala.shopping.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderView {

    private final UUID id;
    private final String orderNumber;
    private final String status;
    private final BigDecimal totalAmount;
    private final BigDecimal canceledAmount;
    private final Instant orderedAt;
    private final List<OrderItemView> items;

    public OrderView(
            UUID id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            Instant orderedAt,
            List<OrderItemView> items
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalAmount = totalAmount;
        this.canceledAmount = canceledAmount;
        this.orderedAt = orderedAt;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getCanceledAmount() {
        return canceledAmount;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public List<OrderItemView> getItems() {
        return items;
    }
}
