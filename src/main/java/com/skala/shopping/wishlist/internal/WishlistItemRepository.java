package com.skala.shopping.wishlist.internal;

import com.skala.shopping.wishlist.internal.domain.WishlistItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId);

    Optional<WishlistItem> findByMemberIdAndProductId(UUID memberId, UUID productId);

    long countByMemberId(UUID memberId);

    void deleteByMemberIdAndProductId(UUID memberId, UUID productId);
}
