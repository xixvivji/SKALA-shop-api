package com.skala.shopping.wishlist.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "WishlistItemRequest", description = "위시리스트 추가 요청")
public final class WishlistItemRequest {

    @Schema(description = "상품 식별자")
    @NotNull
    private UUID productId;

    public WishlistItemRequest() {
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }
}
