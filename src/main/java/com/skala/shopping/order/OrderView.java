package com.skala.shopping.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderView {

    private final UUID id;
    private final String orderNumber;
    private final String status;
    private final String fulfillmentStatus;
    private final BigDecimal totalAmount;
    private final BigDecimal canceledAmount;
    private final BigDecimal remainingPoints;
    private final Instant orderedAt;
    private final List<OrderItemView> items;
    private final ShippingAddressView shippingAddress;

    public OrderView(
            UUID id,
            String orderNumber,
            String status,
            String fulfillmentStatus,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            Instant orderedAt,
            List<OrderItemView> items
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.fulfillmentStatus = fulfillmentStatus;
        this.totalAmount = totalAmount;
        this.canceledAmount = canceledAmount;
        this.remainingPoints = remainingPoints;
        this.orderedAt = orderedAt;
        this.items = items;
        this.shippingAddress = null;
    }

    public OrderView(UUID id, String orderNumber, String status, BigDecimal totalAmount,
                     BigDecimal canceledAmount, BigDecimal remainingPoints, Instant orderedAt,
                     List<OrderItemView> items) {
        this(id, orderNumber, status, "PAID", totalAmount, canceledAmount,
                remainingPoints, orderedAt, items);
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
    public String getFulfillmentStatus() { return fulfillmentStatus; }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getCanceledAmount() {
        return canceledAmount;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public List<OrderItemView> getItems() {
        return items;
    }

    private OrderView(OrderView source, ShippingAddressView shippingAddress) {
        this.id=source.id;this.orderNumber=source.orderNumber;this.status=source.status;
        this.fulfillmentStatus=source.fulfillmentStatus;this.totalAmount=source.totalAmount;
        this.canceledAmount=source.canceledAmount;this.remainingPoints=source.remainingPoints;
        this.orderedAt=source.orderedAt;this.items=source.items;this.shippingAddress=shippingAddress;
    }
    public OrderView withShippingAddress(ShippingAddressView address){return new OrderView(this,address);}
    public ShippingAddressView getShippingAddress(){return shippingAddress;}
}
