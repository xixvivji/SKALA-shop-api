package com.skala.shopping.catalog.internal.web.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class CreateProductVariantRequest {
    @NotBlank @Size(max = 100) private String sku;
    @NotBlank @Size(max = 50) private String optionName;
    @NotBlank @Size(max = 100) private String optionValue;
    @NotNull @DecimalMin("0.00") @DecimalMax("30000000.00") @Digits(integer = 8, fraction = 2)
    private BigDecimal additionalPrice;
    @NotNull @DecimalMin("0") @DecimalMax("1000000") private Integer initialQuantity;
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public String getOptionValue() { return optionValue; }
    public void setOptionValue(String optionValue) { this.optionValue = optionValue; }
    public BigDecimal getAdditionalPrice() { return additionalPrice; }
    public void setAdditionalPrice(BigDecimal additionalPrice) { this.additionalPrice = additionalPrice; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
}
