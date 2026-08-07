package com.skala.shopping.cart.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class UpdateCartItemRequest {
    @Min(1) @Max(1_000_000)
    private int quantity;
    public UpdateCartItemRequest() { }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
