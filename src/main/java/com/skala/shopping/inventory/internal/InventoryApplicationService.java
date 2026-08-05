package com.skala.shopping.inventory.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.inventory.InventoryApi;
import com.skala.shopping.inventory.StockBalance;
import com.skala.shopping.inventory.internal.domain.Stock;
import com.skala.shopping.inventory.internal.domain.StockMovement;
import com.skala.shopping.inventory.internal.domain.StockMovementType;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryApplicationService implements InventoryApi {

    private static final int MAX_OPERATION_QUANTITY = 1_000_000;

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final Clock clock = Clock.systemUTC();

    public InventoryApplicationService(
            StockRepository stockRepository,
            StockMovementRepository movementRepository
    ) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public StockBalance initializeStock(
            UUID productId,
            int availableQuantity,
            UUID operationId
    ) {
        requireIdentifiers(productId, operationId);
        requireNonNegativeQuantity(availableQuantity);
        String fingerprint = fingerprint(
                StockMovementType.INITIALIZE,
                productId,
                availableQuantity,
                null
        );
        StockBalance replay = replayIfProcessed(operationId, productId, fingerprint);
        if (replay != null) {
            return replay;
        }
        Instant now = clock.instant();
        int inserted = stockRepository.insertIfAbsent(productId, availableQuantity, now);
        Stock stock = lockedStock(productId);
        replay = replayIfProcessed(operationId, productId, fingerprint);
        if (replay != null) {
            return replay;
        }
        if (inserted == 0) {
            throw new BusinessException(
                    ErrorCode.DATA_DUPLICATED,
                    "이미 초기화된 상품 재고입니다."
            );
        }
        saveMovement(
                operationId,
                stock,
                StockMovementType.INITIALIZE,
                availableQuantity,
                fingerprint,
                null,
                now
        );
        return stock.toBalance();
    }

    @Transactional(readOnly = true)
    public Optional<StockBalance> findInitializationReplay(
            UUID productId,
            int availableQuantity,
            UUID operationId
    ) {
        requireIdentifiers(productId, operationId);
        requireNonNegativeQuantity(availableQuantity);
        return Optional.ofNullable(replayIfProcessed(
                operationId,
                productId,
                fingerprint(
                        StockMovementType.INITIALIZE,
                        productId,
                        availableQuantity,
                        null
                )
        ));
    }

    @Override
    @Transactional
    public StockBalance reserve(UUID productId, int quantity, UUID operationId) {
        requireIdentifiers(productId, operationId);
        requirePositiveQuantity(quantity);
        return changeStock(
                productId,
                quantity,
                operationId,
                StockMovementType.RESERVE,
                null
        );
    }

    @Override
    @Transactional
    public StockBalance release(UUID productId, int quantity, UUID operationId) {
        requireIdentifiers(productId, operationId);
        requirePositiveQuantity(quantity);
        return changeStock(
                productId,
                quantity,
                operationId,
                StockMovementType.RELEASE,
                null
        );
    }

    @Transactional(readOnly = true)
    public StockBalance getStock(UUID productId) {
        return findStock(productId).toBalance();
    }

    @Transactional(readOnly = true)
    public List<StockBalance> getStocks(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "조회할 상품 ID를 하나 이상 입력해야 합니다."
            );
        }
        if (productIds.size() > 100) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고는 한 번에 최대 100개까지 조회할 수 있습니다."
            );
        }
        Map<UUID, StockBalance> stocksByProduct = new LinkedHashMap<>();
        stockRepository.findAllById(productIds).forEach(stock -> {
            StockBalance balance = stock.toBalance();
            stocksByProduct.put(balance.getProductId(), balance);
        });
        return productIds.stream()
                .distinct()
                .map(stocksByProduct::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public StockBalance adjustStock(
            UUID productId,
            int quantityDelta,
            String reason,
            UUID operationId
    ) {
        requireIdentifiers(productId, operationId);
        if (quantityDelta == 0 || Math.abs((long) quantityDelta) > MAX_OPERATION_QUANTITY) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 조정 수량은 0이 아니며 절댓값이 1,000,000 이하여야 합니다."
            );
        }
        String normalizedReason = normalizeReason(reason);
        StockMovementType type = quantityDelta > 0
                ? StockMovementType.ADJUST_IN
                : StockMovementType.ADJUST_OUT;
        return changeStock(
                productId,
                Math.abs(quantityDelta),
                operationId,
                type,
                normalizedReason
        );
    }

    @Transactional
    public void deactivateStock(UUID productId) {
        stockRepository.deactivateOrInsert(productId, clock.instant());
    }

    private StockBalance changeStock(
            UUID productId,
            int quantity,
            UUID operationId,
            StockMovementType type,
            String reason
    ) {
        String fingerprint = fingerprint(type, productId, quantity, reason);
        StockBalance replay = replayIfProcessed(operationId, productId, fingerprint);
        if (replay != null) {
            return replay;
        }
        Stock stock = lockedStock(productId);
        replay = replayIfProcessed(operationId, productId, fingerprint);
        if (replay != null) {
            return replay;
        }
        Instant now = clock.instant();
        switch (type) {
            case RESERVE -> stock.reserve(quantity, now);
            case RELEASE -> stock.release(quantity, now);
            case ADJUST_IN -> stock.adjustIn(quantity, now);
            case ADJUST_OUT -> stock.adjustOut(quantity, now);
            default -> throw new IllegalStateException("Unsupported stock movement: " + type);
        }
        saveMovement(operationId, stock, type, quantity, fingerprint, reason, now);
        return stock.toBalance();
    }

    private void saveMovement(
            UUID operationId,
            Stock stock,
            StockMovementType type,
            int quantity,
            String fingerprint,
            String reason,
            Instant now
    ) {
        StockBalance balance = stock.toBalance();
        movementRepository.save(new StockMovement(
                operationId,
                balance.getProductId(),
                type,
                quantity,
                balance.getAvailableQuantity(),
                stock.isActive(),
                fingerprint,
                reason,
                now
        ));
    }

    private StockBalance replayIfProcessed(
            UUID operationId,
            UUID productId,
            String fingerprint
    ) {
        return movementRepository.findByOperationIdAndProductId(operationId, productId)
                .map(movement -> {
                    if (!movement.hasFingerprint(fingerprint)) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
                    }
                    return movement.toBalance();
                })
                .orElse(null);
    }

    private Stock lockedStock(UUID productId) {
        return stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DATA_NOT_FOUND,
                        "상품 재고를 찾을 수 없습니다."
                ));
    }

    private Stock findStock(UUID productId) {
        return stockRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DATA_NOT_FOUND,
                        "상품 재고를 찾을 수 없습니다."
                ));
    }

    private String fingerprint(
            StockMovementType type,
            UUID productId,
            int quantity,
            String reason
    ) {
        return type + "|" + productId + "|" + quantity + "|" + (reason == null ? "" : reason);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 조정 사유를 입력해야 합니다."
            );
        }
        String normalized = reason.trim();
        if (normalized.length() > 200) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 조정 사유는 200자 이하여야 합니다."
            );
        }
        return normalized;
    }

    private void requireIdentifiers(UUID productId, UUID operationId) {
        if (productId == null || operationId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0 || quantity > MAX_OPERATION_QUANTITY) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 처리 수량은 1 이상 1,000,000 이하여야 합니다."
            );
        }
    }

    private void requireNonNegativeQuantity(int quantity) {
        if (quantity < 0 || quantity > MAX_OPERATION_QUANTITY) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "초기 재고는 0 이상 1,000,000 이하여야 합니다."
            );
        }
    }
}
