package com.skala.shopping.catalog.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

final class ProductRequest {

    @NotBlank
    @Size(max = 200)
    private String productName;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal productPrice;

    public ProductRequest() {
    }

    public ProductRequest(String productName, BigDecimal productPrice) {
        this.productName = productName;
        this.productPrice = productPrice;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }
}
