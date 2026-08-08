package com.skala.shopping.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** 다른 모듈 또는 외부 메시지 브로커로 전달할 주문 완료 공개 이벤트입니다. */
public final class OrderPlaced {
    private final UUID orderId; private final UUID memberId;
    private final BigDecimal totalAmount; private final Instant occurredAt;
    public OrderPlaced(UUID orderId, UUID memberId, BigDecimal totalAmount, Instant occurredAt) {
        this.orderId=orderId; this.memberId=memberId; this.totalAmount=totalAmount; this.occurredAt=occurredAt;
    }
    public UUID getOrderId(){return orderId;} public UUID getMemberId(){return memberId;}
    public BigDecimal getTotalAmount(){return totalAmount;} public Instant getOccurredAt(){return occurredAt;}
}
