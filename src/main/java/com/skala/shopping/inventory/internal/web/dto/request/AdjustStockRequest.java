package com.skala.shopping.inventory.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "AdjustStockRequest", description = "관리자 재고 조정 요청")
public final class AdjustStockRequest {

    @Schema(
            description = "재고 증감 수량. 입고는 양수, 차감은 음수",
            example = "10"
    )
    @NotNull
    @Min(-1_000_000)
    @Max(1_000_000)
    private Integer quantityDelta;

    @Schema(description = "재고 조정 사유", example = "신규 입고")
    @NotBlank
    @Size(max = 200)
    private String reason;

    public AdjustStockRequest() {
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
