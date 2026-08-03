package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.order.OrderItemView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "StorefrontOrderItemResponse", description = "PDF 호환 주문 항목 응답")
public final class OrderItemResponse {

    @Schema(description = "주문 항목 식별자")
    private final UUID id;

    @Schema(description = "상품 식별자")
    private final UUID productId;

    @Schema(description = "주문 당시 상품명", example = "무선마우스")
    private final String productName;

    @Schema(description = "주문 당시 단가", example = "15000")
    private final BigDecimal unitPrice;

    @Schema(description = "최초 주문 수량", example = "2")
    private final int orderedQuantity;

    @Schema(description = "취소된 수량", example = "0")
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
}
