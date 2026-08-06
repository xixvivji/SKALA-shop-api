package com.skala.shopping.order.internal.web.dto.request;

import com.skala.shopping.order.OrderLineCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public final class OrderLineRequest {
    @NotNull private UUID productId;
    @Min(1) @Max(1_000_000) private int quantity;
    public OrderLineRequest() { }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public OrderLineCommand toCommand() { return new OrderLineCommand(productId, quantity); }
}
