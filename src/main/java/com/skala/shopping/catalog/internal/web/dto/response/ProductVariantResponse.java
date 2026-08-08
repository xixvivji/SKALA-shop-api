package com.skala.shopping.catalog.internal.web.dto.response;

import com.skala.shopping.catalog.ProductVariantSnapshot;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProductVariantResponse {
    private final UUID id; private final UUID productId; private final String sku;
    private final String optionName; private final String optionValue;
    private final BigDecimal price; private final String status;
    public ProductVariantResponse(ProductVariantSnapshot value) {
        id=value.getId(); productId=value.getProductId(); sku=value.getSku();
        optionName=value.getOptionName(); optionValue=value.getOptionValue();
        price=value.getPrice(); status=value.getStatus();
    }
    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getOptionName() { return optionName; }
    public String getOptionValue() { return optionValue; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
}
