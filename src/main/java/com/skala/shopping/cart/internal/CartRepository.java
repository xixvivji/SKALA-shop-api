package com.skala.shopping.cart.internal;

import com.skala.shopping.cart.internal.domain.Cart;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartRepository extends JpaRepository<Cart, UUID> { }
