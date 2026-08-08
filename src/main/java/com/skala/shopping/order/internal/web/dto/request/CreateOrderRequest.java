package com.skala.shopping.order.internal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

@Schema(name = "CreateOrderRequest", description = "주문 생성 요청")
public final class CreateOrderRequest {

    @Schema(description = "상품 식별자")
    private UUID productId;

    @Schema(description = "주문 수량", example = "1", minimum = "1")
    @Min(1)
    @Max(1_000_000)
    private Integer quantity;

    @Valid
    @Size(min = 1, max = 50)
    private List<OrderLineRequest> items;

    @Valid
    private ShippingAddressRequest shippingAddress;

    private String couponCode;

    @DecimalMin(value = "0.00")
    @Digits(integer = 14, fraction = 2)
    @Schema(description = "사용할 포인트. 생략하면 기존 호환 방식으로 전액 포인트 결제", example = "5000.00")
    private BigDecimal pointAmount;

    public CreateOrderRequest() {
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity == null ? 0 : quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<OrderLineRequest> getItems() { return items; }
    public void setItems(List<OrderLineRequest> items) { this.items = items; }
    public ShippingAddressRequest getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddressRequest shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getPointAmount() { return pointAmount; }

    public void setPointAmount(BigDecimal pointAmount) { this.pointAmount = pointAmount; }

    @AssertTrue(message = "productId/quantity 또는 items를 입력해야 합니다.")
    public boolean isOrderShapeValid() {
        boolean legacy = productId != null && quantity != null && quantity > 0;
        boolean multiple = items != null && !items.isEmpty();
        return legacy != multiple;
    }
}
