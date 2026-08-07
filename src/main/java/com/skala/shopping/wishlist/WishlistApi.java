package com.skala.shopping.wishlist;

import java.util.List;
import java.util.UUID;

public interface WishlistApi {

    List<WishlistItemView> getWishlist(UUID memberId);

    WishlistItemView addItem(UUID memberId, UUID productId);

    void removeItem(UUID memberId, UUID productId);
}
