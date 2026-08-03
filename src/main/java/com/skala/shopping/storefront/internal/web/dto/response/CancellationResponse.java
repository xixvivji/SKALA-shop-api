package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.order.CancellationView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "StorefrontCancellationResponse", description = "PDF 호환 상품 취소 결과")
public final class CancellationResponse {

    @Schema(description = "취소 식별자")
    private final UUID id;

    @Schema(description = "상품 식별자")
    private final UUID productId;

    @Schema(description = "취소 수량", example = "1")
    private final int canceledQuantity;

    @Schema(description = "환급 포인트", example = "15000")
    private final BigDecimal refundAmount;

    @Schema(description = "취소 후 포인트 잔액", example = "985000")
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

    public static CancellationResponse from(CancellationView cancellation) {
        return new CancellationResponse(
                cancellation.getId(),
                cancellation.getProductId(),
                cancellation.getCanceledQuantity(),
                cancellation.getRefundAmount(),
                cancellation.getRemainingPoints()
        );
    }
}
