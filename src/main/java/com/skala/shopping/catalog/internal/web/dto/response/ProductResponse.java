package com.skala.shopping.catalog.internal.web.dto.response;

import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "ProductResponse", description = "상품 응답")
public final class ProductResponse {

    @Schema(description = "상품 식별자")
    private final UUID id;

    @Schema(description = "상품명", example = "무선마우스")
    private final String name;

    @Schema(description = "상품 가격", example = "15000")
    private final BigDecimal price;

    @Schema(description = "상품 상태", example = "ACTIVE")
    private final String status;

    public ProductResponse(UUID id, String name, BigDecimal price, String status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public static ProductResponse from(ProductSnapshot product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStatus()
        );
    }

    public static PageResponse<ProductResponse> pageFrom(PageResponse<ProductSnapshot> products) {
        return new PageResponse<>(
                products.getContent().stream().map(ProductResponse::from).toList(),
                products.getPage(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
    }
}
