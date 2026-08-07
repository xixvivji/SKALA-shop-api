package com.skala.shopping.inventory;

import java.time.Instant;
import java.util.UUID;

/** 재고가 0개에서 주문 가능한 상태로 전환됐음을 다른 모듈에 알리는 공개 이벤트입니다. */
public final class StockReplenished {

    private final UUID productId;
    private final int availableQuantity;
    private final Instant occurredAt;

    public StockReplenished(UUID productId, int availableQuantity, Instant occurredAt) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.occurredAt = occurredAt;
    }

    public UUID getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public Instant getOccurredAt() { return occurredAt; }
}
