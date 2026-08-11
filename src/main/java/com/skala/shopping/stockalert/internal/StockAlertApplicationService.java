package com.skala.shopping.stockalert.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.inventory.InventoryApi;
import com.skala.shopping.stockalert.StockAlertApi;
import com.skala.shopping.stockalert.StockAlertResponse;
import com.skala.shopping.stockalert.StockAlertTriggered;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAlertApplicationService implements StockAlertApi {

    private final StockAlertRepository repository;
    private final CatalogApi catalogApi;
    private final InventoryApi inventoryApi;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.systemUTC();

    StockAlertApplicationService(
            StockAlertRepository repository,
            CatalogApi catalogApi,
            InventoryApi inventoryApi,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.catalogApi = catalogApi;
        this.inventoryApi = inventoryApi;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StockAlertResponse subscribe(UUID memberId, UUID productId) {
        catalogApi.getSaleableProduct(productId);
        int availableQuantity = inventoryApi.getStock(productId).getAvailableQuantity();
        if (availableQuantity > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "현재 구매 가능한 상품은 재입고 알림을 신청할 수 없습니다."
            );
        }
        var existing = repository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isPresent()) {
            if (existing.get().notifiedAt() != null) {
                existing.get().rearm();
            }
            return toResponse(existing.get());
        }
        return toResponse(repository.save(new com.skala.shopping.stockalert.internal.domain.StockAlertSubscription(
                memberId,
                productId,
                clock.instant()
        )));
    }

    @Override
    @Transactional
    public void unsubscribe(UUID memberId, UUID productId) {
        var existing = repository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "재입고 알림을 찾을 수 없습니다."));
        repository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAlertResponse> getSubscriptions(UUID memberId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<com.skala.shopping.stockalert.internal.domain.StockAlertSubscription> subscriptions =
                repository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId, pageable);

        if (subscriptions.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    subscriptions.getNumber(),
                    subscriptions.getSize(),
                    subscriptions.getTotalElements(),
                    subscriptions.getTotalPages()
            );
        }

        List<UUID> productIds = subscriptions.getContent()
                .stream()
                .map(com.skala.shopping.stockalert.internal.domain.StockAlertSubscription::productId)
                .toList();
        Map<UUID, ProductSnapshot> products = catalogApi.getSaleableProducts(productIds).stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, product -> product));

        List<StockAlertResponse> content = subscriptions.getContent().stream()
                .map(subscription -> {
                    ProductSnapshot product = products.get(subscription.productId());
                    int available = availableNow(subscription.productId());
                    String productName = product == null ? "알 수 없는 상품" : product.getName();
                    return subscription.toResponse(productName, available);
                }).toList();

        return new PageResponse<>(
                content,
                subscriptions.getNumber(),
                subscriptions.getSize(),
                subscriptions.getTotalElements(),
                subscriptions.getTotalPages()
        );
    }

    @Transactional
    public void notifySubscribers(UUID productId, int availableQuantity, Instant occurredAt) {
        if (availableQuantity <= 0) {
            return;
        }
        repository.findAllByProductIdAndNotifiedAtIsNull(productId)
                .forEach(subscription -> {
                    if (subscription.markNotified(availableQuantity, occurredAt)) {
                        eventPublisher.publishEvent(new StockAlertTriggered(
                                subscription.id(),
                                subscription.memberId(),
                                subscription.productId(),
                                availableQuantity,
                                occurredAt
                        ));
                    }
                });
    }

    private StockAlertResponse toResponse(com.skala.shopping.stockalert.internal.domain.StockAlertSubscription subscription) {
        ProductSnapshot product;
        try {
            product = catalogApi.getSaleableProduct(subscription.productId());
        } catch (BusinessException exception) {
            product = null;
        }
        String productName = product == null ? "알 수 없는 상품" : product.getName();
        return subscription.toResponse(productName, availableNow(subscription.productId()));
    }

    private int availableNow(UUID productId) {
        try {
            return inventoryApi.getStock(productId).getAvailableQuantity();
        } catch (BusinessException exception) {
            if (exception.errorCode() == ErrorCode.DATA_NOT_FOUND) {
                return 0;
            }
            throw exception;
        }
    }
}
