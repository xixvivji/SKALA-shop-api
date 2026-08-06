package com.skala.shopping.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartViewTests {
    @Test void calculatesDistinctCountQuantityAndAmount() {
        CartView cart = new CartView(List.of(
                new CartItemView(UUID.randomUUID(), "첫 상품", new BigDecimal("1000.00"), 2, 10, true),
                new CartItemView(UUID.randomUUID(), "둘째 상품", new BigDecimal("2500.00"), 3, 3, true)
        ));
        assertEquals(2, cart.getItemCount());
        assertEquals(5, cart.getTotalQuantity());
        assertEquals(0, new BigDecimal("9500.00").compareTo(cart.getTotalAmount()));
    }
}
