package com.skala.shopping.catalog.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "UpdateProductRequest", description = "상품 수정 요청")
public final class UpdateProductRequest {

    @Schema(description = "상품명", example = "무선마우스")
    @NotBlank
    @Size(max = 200)
    private String productName;

    @Schema(
            description = "상품 가격. 최대 주문 수량의 합계도 JavaScript에서 센트 단위로 안전하게 표현할 수 있습니다.",
            example = "15000.00",
            minimum = "0.01",
            maximum = "30000000.00"
    )
    @NotNull
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "30000000.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal productPrice;
    private UUID categoryId;
    @Size(max=2000) private String description;
    @Size(max=1000) private String imageUrl;

    public UpdateProductRequest() {
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
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId=categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description=description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl=imageUrl; }
}
