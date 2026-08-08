package com.skala.shopping.order.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import java.util.UUID;

@Schema(name = "CancelOrderRequest", description = "주문 항목 부분 취소 요청")
public final class CancelOrderRequest {

    @Schema(description = "취소할 주문 항목 식별자. 주문 조회 응답의 items[].id 값")
    private UUID orderItemId;

    @Schema(description = "이전 단순상품 API 호환용 상품 식별자", deprecated = true)
    private UUID productId;

    @Schema(description = "취소 수량", example = "1", minimum = "1")
    @Min(1)
    @Max(1_000_000)
    private int quantity;

    public CancelOrderRequest() {
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @AssertTrue(message = "orderItemId 또는 호환용 productId 중 하나만 입력해야 합니다.")
    public boolean isTargetSpecified() {
        return (orderItemId == null) != (productId == null);
    }
}
