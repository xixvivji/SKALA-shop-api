package com.skala.shopping.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/** 주문과 장바구니가 카탈로그 내부 엔티티에 의존하지 않도록 제공하는 SKU 읽기 모델입니다. */
public final class ProductVariantSnapshot {
    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String sku;
    private final String optionName;
    private final String optionValue;
    private final BigDecimal price;
    private final String status;

    public ProductVariantSnapshot(UUID id, UUID productId, String productName, String sku,
                                  String optionName, String optionValue, BigDecimal price, String status) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.optionName = optionName;
        this.optionValue = optionValue;
        this.price = price;
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSku() { return sku; }
    public String getOptionName() { return optionName; }
    public String getOptionValue() { return optionValue; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public String getDisplayName() {
        return optionValue == null ? productName : productName + " (" + optionName + ": " + optionValue + ")";
    }
}
