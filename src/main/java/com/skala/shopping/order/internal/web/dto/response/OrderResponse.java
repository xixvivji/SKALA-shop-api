package com.skala.shopping.order.internal.web.dto.response;

import com.skala.shopping.order.OrderView;
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
    private final BigDecimal totalAmount;
    private final BigDecimal canceledAmount;
    private final BigDecimal remainingPoints;
    private final Instant orderedAt;
    private final List<OrderItemResponse> items;

    public OrderResponse(
            UUID id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            BigDecimal remainingPoints,
            Instant orderedAt,
            List<OrderItemResponse> items
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.totalAmount = totalAmount;
        this.canceledAmount = canceledAmount;
        this.remainingPoints = remainingPoints;
        this.orderedAt = orderedAt;
        this.items = items;
    }

    public static OrderResponse from(OrderView order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCanceledAmount(),
                order.getRemainingPoints(),
                order.getOrderedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
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

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
