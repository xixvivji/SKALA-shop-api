package com.skala.shopping.order.internal.domain;

import com.skala.shopping.order.OrderItemView;
import com.skala.shopping.order.OrderView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "orders")
public class ShopOrder {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "canceled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal canceledAmount;

    @Version
    private long version;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopOrder() {
    }

    public ShopOrder(
            UUID id,
            UUID requestId,
            String orderNumber,
            UUID memberId,
            BigDecimal totalAmount,
            Instant now
    ) {
        this.id = id;
        this.requestId = requestId;
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.status = OrderStatus.PAID;
        this.totalAmount = totalAmount;
        this.canceledAmount = BigDecimal.ZERO;
        this.orderedAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID memberId() {
        return memberId;
    }

    public Instant orderedAt() {
        return orderedAt;
    }

    public void applyCancellation(BigDecimal amount, boolean fullyCanceled, Instant now) {
        canceledAmount = canceledAmount.add(amount);
        status = fullyCanceled ? OrderStatus.CANCELED : OrderStatus.PARTIALLY_CANCELED;
        updatedAt = now;
    }

    public OrderView toView(List<OrderItemView> items) {
        return new OrderView(
                id,
                orderNumber,
                status.name(),
                totalAmount,
                canceledAmount,
                orderedAt,
                items
        );
    }
}
