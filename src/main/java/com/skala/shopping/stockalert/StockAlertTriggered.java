package com.skala.shopping.stockalert;

import java.time.Instant;
import java.util.UUID;

/** 회원별 재입고 알림 전달을 외부 서비스에 위임하기 위한 공개 이벤트입니다. */
public final class StockAlertTriggered {

    private final UUID subscriptionId;
    private final UUID memberId;
    private final UUID productId;
    private final int availableQuantity;
    private final Instant occurredAt;

    public StockAlertTriggered(
            UUID subscriptionId,
            UUID memberId,
            UUID productId,
            int availableQuantity,
            Instant occurredAt
    ) {
        this.subscriptionId = subscriptionId;
        this.memberId = memberId;
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.occurredAt = occurredAt;
    }

    public UUID getSubscriptionId() { return subscriptionId; }
    public UUID getMemberId() { return memberId; }
    public UUID getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public Instant getOccurredAt() { return occurredAt; }
}
