package com.skala.shopping.cart;

import java.math.BigDecimal;
import java.util.List;

public final class CartView {
    private final List<CartItemView> items;

    public CartView(List<CartItemView> items) {
        this.items = List.copyOf(items);
    }

    public List<CartItemView> getItems() { return items; }
    public int getItemCount() { return items.size(); }
    public int getTotalQuantity() { return items.stream().mapToInt(CartItemView::getQuantity).sum(); }
    public BigDecimal getTotalAmount() {
        return items.stream().map(CartItemView::getLineAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
