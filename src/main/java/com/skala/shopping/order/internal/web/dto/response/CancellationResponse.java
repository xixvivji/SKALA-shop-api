package com.skala.shopping.order.internal.web.dto.response;

import com.skala.shopping.order.CancellationView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CancellationResponse", description = "상품 부분 취소 응답")
public final class CancellationResponse {

    private final UUID id;
    private final UUID productId;
    private final int canceledQuantity;
    private final BigDecimal refundAmount;
    private final BigDecimal remainingPoints;

    public CancellationResponse(
            UUID id,
            UUID productId,
            int canceledQuantity,
            BigDecimal refundAmount,
            BigDecimal remainingPoints
    ) {
        this.id = id;
        this.productId = productId;
        this.canceledQuantity = canceledQuantity;
        this.refundAmount = refundAmount;
        this.remainingPoints = remainingPoints;
    }

    public static CancellationResponse from(CancellationView cancellation) {
        return new CancellationResponse(
                cancellation.getId(),
                cancellation.getProductId(),
                cancellation.getCanceledQuantity(),
                cancellation.getRefundAmount(),
                cancellation.getRemainingPoints()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getCanceledQuantity() {
        return canceledQuantity;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public BigDecimal getRemainingPoints() {
        return remainingPoints;
    }
}
