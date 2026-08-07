package com.skala.shopping.cart.internal;

import com.skala.shopping.cart.CartApi;
import com.skala.shopping.cart.CartItemView;
import com.skala.shopping.cart.CartView;
import com.skala.shopping.cart.internal.domain.Cart;
import com.skala.shopping.cart.internal.domain.CartItem;
import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductDeleted;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.inventory.InventoryApi;
import com.skala.shopping.inventory.StockBalance;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartApplicationService implements CartApi {
    private static final int MAX_DISTINCT_ITEMS = 50;
    private static final int MAX_QUANTITY = 1_000_000;

    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final CatalogApi catalogApi;
    private final InventoryApi inventoryApi;
    private final Clock clock = Clock.systemUTC();

    public CartApplicationService(CartRepository cartRepository, CartItemRepository itemRepository,
                                  CatalogApi catalogApi, InventoryApi inventoryApi) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.catalogApi = catalogApi;
        this.inventoryApi = inventoryApi;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(UUID memberId) {
        requireMemberId(memberId);
        return assemble(memberId);
    }

    @Transactional
    public CartView addItem(UUID memberId, UUID productId, int quantity) {
        requireMemberId(memberId);
        requireQuantity(quantity);
        ProductSnapshot product = catalogApi.getSaleableProduct(productId);
        StockBalance stock = inventoryApi.getStock(productId);
        CartItem existing = itemRepository.findByMemberIdAndProductId(memberId, productId).orElse(null);
        int newQuantity = existing == null ? quantity : Math.addExact(existing.getQuantity(), quantity);
        requireQuantity(newQuantity);
        requireOrderable(stock, newQuantity);
        Instant now = clock.instant();
        Cart cart = cartRepository.findById(memberId).orElseGet(() -> cartRepository.save(new Cart(memberId, now)));
        if (existing == null) {
            if (itemRepository.countByMemberId(memberId) >= MAX_DISTINCT_ITEMS) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "장바구니에는 최대 50종의 상품을 담을 수 있습니다.");
            }
            itemRepository.save(new CartItem(memberId, product.getId(), quantity, now));
        } else {
            existing.changeQuantity(newQuantity, now);
        }
        cart.touch(now);
        return assemble(memberId);
    }

    @Transactional
    public CartView updateItem(UUID memberId, UUID productId, int quantity) {
        requireMemberId(memberId);
        requireQuantity(quantity);
        catalogApi.getSaleableProduct(productId);
        requireOrderable(inventoryApi.getStock(productId), quantity);
        CartItem item = itemRepository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
        item.changeQuantity(quantity, clock.instant());
        return assemble(memberId);
    }

    @Transactional
    public CartView removeItem(UUID memberId, UUID productId) {
        CartItem item = itemRepository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "장바구니 상품을 찾을 수 없습니다."));
        itemRepository.delete(item);
        itemRepository.flush();
        return assemble(memberId);
    }

    @Override
    @Transactional
    public CartView clearCart(UUID memberId) {
        requireMemberId(memberId);
        itemRepository.deleteAllByMemberId(memberId);
        return new CartView(List.of());
    }

    @EventListener
    @Transactional
    public void removeDeletedProduct(ProductDeleted event) {
        itemRepository.deleteAllByProductId(event.getProductId());
    }

    private CartView assemble(UUID memberId) {
        List<CartItem> items = itemRepository.findAllByMemberIdOrderByCreatedAtAscIdAsc(memberId);
        if (items.isEmpty()) return new CartView(List.of());
        List<UUID> ids = items.stream().map(CartItem::getProductId).toList();
        Map<UUID, ProductSnapshot> products = catalogApi.getSaleableProducts(ids).stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, Function.identity()));
        Map<UUID, StockBalance> stocks = inventoryApi.getStocks(ids).stream()
                .collect(Collectors.toMap(StockBalance::getProductId, Function.identity()));
        return new CartView(items.stream().map(item -> {
            ProductSnapshot product = products.get(item.getProductId());
            StockBalance stock = stocks.get(item.getProductId());
            int available = stock == null ? 0 : stock.getAvailableQuantity();
            boolean orderable = stock != null && stock.isOrderable()
                    && item.getQuantity() <= stock.getMaxOrderQuantity();
            return new CartItemView(item.getProductId(), product.getName(), product.getPrice(),
                    item.getQuantity(), available, orderable);
        }).toList());
    }

    private void requireMemberId(UUID memberId) {
        if (memberId == null) throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
    }
    private void requireQuantity(int quantity) {
        if (quantity <= 0 || quantity > MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "수량은 1 이상 1,000,000 이하여야 합니다.");
        }
    }
    private void requireOrderable(StockBalance stock, int quantity) {
        if (!stock.isOrderable() || quantity > stock.getMaxOrderQuantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }
}
