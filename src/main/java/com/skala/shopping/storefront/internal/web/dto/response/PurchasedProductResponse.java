package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.order.PurchasedProductView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "PurchasedProductResponse", description = "취소되지 않고 보유 중인 구매 상품")
public final class PurchasedProductResponse {

    @Schema(description = "상품 식별자")
    private final UUID productId;

    @Schema(description = "주문 당시 상품명", example = "무선마우스")
    private final String productName;

    @Schema(description = "가장 최근 주문 단가", example = "15000")
    private final BigDecimal latestUnitPrice;

    @Schema(description = "취소되지 않은 구매 수량", example = "2")
    private final int quantity;

    public PurchasedProductResponse(
            UUID productId,
            String productName,
            BigDecimal latestUnitPrice,
            int quantity
    ) {
        this.productId = productId;
        this.productName = productName;
        this.latestUnitPrice = latestUnitPrice;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getLatestUnitPrice() {
        return latestUnitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public static PurchasedProductResponse from(PurchasedProductView product) {
        return new PurchasedProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getLatestUnitPrice(),
                product.getQuantity()
        );
    }
}
