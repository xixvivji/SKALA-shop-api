package com.skala.shopping.cart;

import java.util.UUID;

public interface CartApi {
    CartView getCart(UUID memberId);
    CartView clearCart(UUID memberId);
}
