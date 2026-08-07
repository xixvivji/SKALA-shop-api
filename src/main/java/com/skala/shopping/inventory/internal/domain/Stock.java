package com.skala.shopping.inventory.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.inventory.StockBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stocks", schema = "inventory")
public class Stock {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Stock() {
    }

    public Stock(UUID productId, int availableQuantity, Instant now) {
        if (availableQuantity < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "초기 재고는 0 이상이어야 합니다."
            );
        }
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.status = StockStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void reserve(int quantity, Instant now) {
        requireActive();
        requirePositiveQuantity(quantity);
        if (quantity > availableQuantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        availableQuantity -= quantity;
        updatedAt = now;
    }

    public void release(int quantity, Instant now) {
        requirePositiveQuantity(quantity);
        changeBy(quantity, now);
    }

    public void adjustIn(int quantity, Instant now) {
        requireActive();
        release(quantity, now);
    }

    public void adjustOut(int quantity, Instant now) {
        requireActive();
        reserve(quantity, now);
    }

    public void deactivate(Instant now) {
        status = StockStatus.INACTIVE;
        updatedAt = now;
    }

    public StockBalance toBalance() {
        return new StockBalance(
                productId,
                availableQuantity,
                status == StockStatus.ACTIVE
        );
    }

    public boolean isActive() {
        return status == StockStatus.ACTIVE;
    }

    private void changeBy(int quantityDelta, Instant now) {
        long changedQuantity = (long) availableQuantity + quantityDelta;
        if (changedQuantity < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        if (changedQuantity > Integer.MAX_VALUE) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 수량이 허용 범위를 초과합니다."
            );
        }
        availableQuantity = (int) changedQuantity;
        updatedAt = now;
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "재고 처리 수량은 1 이상이어야 합니다."
            );
        }
    }

    private void requireActive() {
        if (status != StockStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_SALEABLE,
                    "비활성화된 상품의 재고는 조정할 수 없습니다."
            );
        }
    }
}
