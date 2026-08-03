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
    private final int orderedQuantity;
    private final int canceledQuantity;

    public OrderItemResponse(
            UUID id,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity,
            int canceledQuantity
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.orderedQuantity = orderedQuantity;
        this.canceledQuantity = canceledQuantity;
    }

    public static OrderItemResponse from(OrderItemView item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
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

    public int getOrderedQuantity() {
        return orderedQuantity;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }
}
