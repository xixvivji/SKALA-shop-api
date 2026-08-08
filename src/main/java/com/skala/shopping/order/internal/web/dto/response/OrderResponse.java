package com.skala.shopping.order.internal.web.dto.response;

import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.OrderStatusHistoryView;
import com.skala.shopping.order.OrderView;
import com.skala.shopping.order.ShippingAddressView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "OrderResponse", description = "주문 응답")
public final class OrderResponse {

    private final UUID id;
    private final String orderNumber;
    private final String status;
    private final String fulfillmentStatus;
    private final BigDecimal totalAmount;
    private final BigDecimal canceledAmount;
    private final BigDecimal remainingPoints;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal pointUsedAmount;
    private final BigDecimal paymentAmount;
    private final String usedCouponCode;
    private final String trackingCarrier;
    private final String trackingNumber;
    private final String trackingUrl;
    private final Instant estimatedDeliveryAt;
    private final Instant orderedAt;
    private final List<OrderItemResponse> items;
    private final List<OrderStatusHistoryView> statusHistory;
    private final ShippingAddressView shippingAddress;

    public OrderResponse(
            UUID id,
            String orderNumber,
            String status,
            String fulfillmentStatus,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String usedCouponCode,
            String trackingCarrier,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            Instant orderedAt,
            List<OrderItemResponse> items
    ) {
        this(
                id,
                orderNumber,
                status,
                fulfillmentStatus,
                totalAmount,
                canceledAmount,
                remainingPoints,
                originalAmount,
                discountAmount,
                totalAmount,
                BigDecimal.ZERO,
                usedCouponCode,
                trackingCarrier,
                trackingNumber,
                trackingUrl,
                estimatedDeliveryAt,
                orderedAt,
                items,
                List.of(),
                null
        );
    }

    private OrderResponse(
            UUID id,
            String orderNumber,
            String status,
            String fulfillmentStatus,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal pointUsedAmount,
            BigDecimal paymentAmount,
            String usedCouponCode,
            String trackingCarrier,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            Instant orderedAt,
            List<OrderItemResponse> items,
            List<OrderStatusHistoryView> statusHistory,
            ShippingAddressView shippingAddress
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.fulfillmentStatus = fulfillmentStatus;
        this.totalAmount = totalAmount;
        this.canceledAmount = canceledAmount;
        this.remainingPoints = remainingPoints;
        this.originalAmount = originalAmount == null ? totalAmount : originalAmount;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.pointUsedAmount = pointUsedAmount == null ? totalAmount : pointUsedAmount;
        this.paymentAmount = paymentAmount == null ? BigDecimal.ZERO : paymentAmount;
        this.usedCouponCode = usedCouponCode;
        this.trackingCarrier = trackingCarrier;
        this.trackingNumber = trackingNumber;
        this.trackingUrl = trackingUrl;
        this.estimatedDeliveryAt = estimatedDeliveryAt;
        this.orderedAt = orderedAt;
        this.items = items;
        this.statusHistory = statusHistory == null ? List.of() : statusHistory;
        this.shippingAddress = shippingAddress;
    }

    public static OrderResponse from(OrderView order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getFulfillmentStatus(),
                order.getTotalAmount(),
                order.getCanceledAmount(),
                order.getRemainingPoints(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getPointUsedAmount(),
                order.getPaymentAmount(),
                order.getUsedCouponCode(),
                order.getTrackingCarrier(),
                order.getTrackingNumber(),
                order.getTrackingUrl(),
                order.getEstimatedDeliveryAt(),
                order.getOrderedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getStatusHistory(),
                order.getShippingAddress()
        );
    }

    public static PageResponse<OrderResponse> pageFrom(PageResponse<OrderView> orders) {
        return new PageResponse<>(
                orders.getContent().stream().map(OrderResponse::from).toList(),
                orders.getPage(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
        );
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

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPointUsedAmount() { return pointUsedAmount; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }

    public String getUsedCouponCode() {
        return usedCouponCode;
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

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public List<OrderStatusHistoryView> getStatusHistory() {
        return statusHistory;
    }

    public ShippingAddressView getShippingAddress() {
        return shippingAddress;
    }
}
