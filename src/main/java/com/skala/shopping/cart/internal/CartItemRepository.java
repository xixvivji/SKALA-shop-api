package com.skala.shopping.cart.internal;

import com.skala.shopping.cart.internal.domain.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findAllByMemberIdOrderByCreatedAtAscIdAsc(UUID memberId);
    Optional<CartItem> findByMemberIdAndVariantId(UUID memberId, UUID variantId);
    long countByMemberId(UUID memberId);
    void deleteAllByMemberId(UUID memberId);
    void deleteAllByProductId(UUID productId);
    void deleteAllByVariantId(UUID variantId);
}
