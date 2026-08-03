package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.order.OrderView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "OrderCompatibilityResponse", description = "PDF 호환 주문 및 포인트 결과")
public final class OrderCompatibilityResponse {

    @Schema(description = "생성된 주문")
    private final OrderResponse order;

    @Schema(description = "주문 후 포인트 잔액", example = "970000")
    private final BigDecimal remainingPoints;

    public OrderCompatibilityResponse(OrderResponse order, BigDecimal remainingPoints) {
        this.order = order;
        this.remainingPoints = remainingPoints;
    }

    public OrderResponse getOrder() {
        return order;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }

    public static OrderCompatibilityResponse from(OrderView order, BigDecimal remainingPoints) {
        return new OrderCompatibilityResponse(OrderResponse.from(order), remainingPoints);
    }
}
