package com.skala.shopping.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;

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
    private final String usedCouponCode;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final String trackingCarrier;
    private final String trackingNumber;
    private final String trackingUrl;
    private final Instant estimatedDeliveryAt;
    private final List<OrderStatusHistoryView> statusHistory;

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
        this(
                id,
                orderNumber,
                status,
                fulfillmentStatus,
                totalAmount,
                canceledAmount,
                remainingPoints,
                orderedAt,
                items,
                null,
                totalAmount,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                emptyList()
        );
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

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

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

    public String getUsedCouponCode() {
        return usedCouponCode;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getTrackingCarrier() {
        return trackingCarrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public Instant getEstimatedDeliveryAt() {
        return estimatedDeliveryAt;
    }

    public List<OrderStatusHistoryView> getStatusHistory() {
        return statusHistory;
    }

    public ShippingAddressView getShippingAddress() {
        return shippingAddress;
    }

    public OrderView withShippingAddress(ShippingAddressView address) {
        return new OrderView(this, address);
    }

    public OrderView withCoupon(String usedCouponCode, BigDecimal originalAmount, BigDecimal discountAmount) {
        return new OrderView(
                this.id,
                this.orderNumber,
                this.status,
                this.fulfillmentStatus,
                this.totalAmount,
                this.canceledAmount,
                this.remainingPoints,
                this.orderedAt,
                this.items,
                usedCouponCode,
                originalAmount,
                discountAmount,
                this.trackingCarrier,
                this.trackingNumber,
                this.trackingUrl,
                this.estimatedDeliveryAt,
                this.statusHistory
        );
    }

    public OrderView withTracking(String carrier, String number, String url, Instant estimatedDeliveryAt) {
        return new OrderView(
                this.id,
                this.orderNumber,
                this.status,
                this.fulfillmentStatus,
                this.totalAmount,
                this.canceledAmount,
                this.remainingPoints,
                this.orderedAt,
                this.items,
                this.usedCouponCode,
                this.originalAmount,
                this.discountAmount,
                carrier,
                number,
                url,
                estimatedDeliveryAt,
                this.statusHistory
        );
    }

    public OrderView withStatusHistory(List<OrderStatusHistoryView> statusHistory) {
        return new OrderView(
                this.id,
                this.orderNumber,
                this.status,
                this.fulfillmentStatus,
                this.totalAmount,
                this.canceledAmount,
                this.remainingPoints,
                this.orderedAt,
                this.items,
                this.usedCouponCode,
                this.originalAmount,
                this.discountAmount,
                this.trackingCarrier,
                this.trackingNumber,
                this.trackingUrl,
                this.estimatedDeliveryAt,
                statusHistory == null ? new ArrayList<>() : statusHistory
        );
    }

    private OrderView(
            UUID id,
            String orderNumber,
            String status,
            String fulfillmentStatus,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            Instant orderedAt,
            List<OrderItemView> items,
            String usedCouponCode,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String trackingCarrier,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            List<OrderStatusHistoryView> statusHistory
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
        this.usedCouponCode = usedCouponCode;
        this.originalAmount = originalAmount == null ? totalAmount : originalAmount;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.trackingCarrier = trackingCarrier;
        this.trackingNumber = trackingNumber;
        this.trackingUrl = trackingUrl;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
        this.statusHistory = statusHistory == null ? emptyList() : statusHistory;
    }

    private OrderView(OrderView source, ShippingAddressView shippingAddress) {
        this(
                source.id,
                source.orderNumber,
                source.status,
                source.fulfillmentStatus,
                source.totalAmount,
                source.canceledAmount,
                source.remainingPoints,
                source.orderedAt,
                source.items,
                source.usedCouponCode,
                source.originalAmount,
                source.discountAmount,
                source.trackingCarrier,
                source.trackingNumber,
                source.trackingUrl,
                source.estimatedDeliveryAt,
                source.shippingAddress,
                source.statusHistory
        );
    }

    private OrderView(
            UUID id,
            String orderNumber,
            String status,
            String fulfillmentStatus,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            Instant orderedAt,
            List<OrderItemView> items,
            String usedCouponCode,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String trackingCarrier,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            ShippingAddressView shippingAddress,
            List<OrderStatusHistoryView> statusHistory
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
        this.shippingAddress = shippingAddress;
        this.usedCouponCode = usedCouponCode;
        this.originalAmount = originalAmount == null ? totalAmount : originalAmount;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.trackingCarrier = trackingCarrier;
        this.trackingNumber = trackingNumber;
        this.trackingUrl = trackingUrl;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
        this.statusHistory = statusHistory == null ? emptyList() : statusHistory;
    }
}
