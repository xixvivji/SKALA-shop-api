package com.skala.shopping.wishlist.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.wishlist.WishlistApi;
import com.skala.shopping.wishlist.WishlistItemView;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistApplicationService implements WishlistApi {

    private static final int MAX_WISHLIST_ITEMS = 100;
    private final WishlistItemRepository repository;
    private final CatalogApi catalogApi;
    private final Clock clock = Clock.systemUTC();

    public WishlistApplicationService(
            WishlistItemRepository repository,
            CatalogApi catalogApi
    ) {
        this.repository = repository;
        this.catalogApi = catalogApi;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemView> getWishlist(UUID memberId) {
        List<WishlistItem> items = repository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductSnapshot> products = catalogApi.getSaleableProducts(
                items.stream().map(WishlistItem::productId).toList()
        ).stream().collect(Collectors.toMap(ProductSnapshot::getId, product -> product));
        return items.stream().map(item -> item.toView(products.get(item.productId()))).toList();
    }

    @Override
    @Transactional
    public WishlistItemView addItem(UUID memberId, UUID productId) {
        requireMemberId(memberId);
        requireProductId(productId);
        catalogApi.getSaleableProduct(productId);
        if (repository.findByMemberIdAndProductId(memberId, productId).isPresent()) {
            return repository.findByMemberIdAndProductId(memberId, productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR))
                    .toView(catalogApi.getSaleableProduct(productId));
        }
        if (repository.countByMemberId(memberId) >= MAX_WISHLIST_ITEMS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "위시리스트는 최대 100개까지 저장할 수 있습니다.");
        }
        Instant now = clock.instant();
        WishlistItem item = repository.save(new WishlistItem(memberId, productId, now));
        return item.toView(catalogApi.getSaleableProduct(productId));
    }

    @Override
    @Transactional
    public void removeItem(UUID memberId, UUID productId) {
        WishlistItem item = repository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "위시리스트에 해당 상품이 없습니다."));
        repository.delete(item);
    }

    private void requireMemberId(UUID memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }

    private void requireProductId(UUID productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "상품 ID가 필요합니다.");
        }
    }
}
