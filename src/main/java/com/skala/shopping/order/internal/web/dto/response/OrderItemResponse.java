package com.skala.shopping.order.internal.web.dto.response;

import com.skala.shopping.order.OrderItemView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "OrderItemResponse", description = "주문 항목 응답")
public final class OrderItemResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final BigDecimal paidAmount;
    private final BigDecimal refundedAmount;
    private final int orderedQuantity;
    private final int canceledQuantity;

    public OrderItemResponse(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            int orderedQuantity,
            int canceledQuantity
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.paidAmount = paidAmount;
        this.refundedAmount = refundedAmount;
        this.orderedQuantity = orderedQuantity;
        this.canceledQuantity = canceledQuantity;
    }

    public static OrderItemResponse from(OrderItemView item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getPaidAmount(),
                item.getRefundedAmount(),
                item.getOrderedQuantity(),
                item.getCanceledQuantity()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getPaidAmount() { return paidAmount; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }
}
