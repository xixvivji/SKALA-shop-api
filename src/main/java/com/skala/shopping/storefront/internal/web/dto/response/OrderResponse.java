package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.order.OrderView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "StorefrontOrderResponse", description = "PDF 호환 주문 응답")
public final class OrderResponse {

    @Schema(description = "주문 식별자")
    private final UUID id;

    @Schema(description = "고객에게 표시할 주문 번호", example = "SKALA-20260803-A1B2C3D4E5F6")
    private final String orderNumber;

    @Schema(description = "주문 상태", example = "PAID")
    private final String status;

    @Schema(description = "최초 결제 금액", example = "30000")
    private final BigDecimal totalAmount;

    @Schema(description = "누적 취소 금액", example = "0")
    private final BigDecimal canceledAmount;

    @Schema(description = "주문 시각")
    private final Instant orderedAt;

    @Schema(description = "주문 항목")
    private final List<OrderItemResponse> items;

    public OrderResponse(
            UUID id,
            String orderNumber,
            String status,
            BigDecimal totalAmount,
            BigDecimal canceledAmount,
            Instant orderedAt,
            List<OrderItemResponse> items
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

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public static OrderResponse from(OrderView order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCanceledAmount(),
                order.getOrderedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
