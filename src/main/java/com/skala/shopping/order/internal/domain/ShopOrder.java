package com.skala.shopping.order.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.OrderItemView;
import com.skala.shopping.order.OrderView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        schema = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_member_request",
                columnNames = {"member_id", "request_id"}
        )
)
public class ShopOrder {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "request_fingerprint", nullable = false, length = 2048)
    private String requestFingerprint;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false, length = 30)
    private FulfillmentStatus fulfillmentStatus;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "used_coupon_code", length = 50)
    private String usedCouponCode;

    @Column(name = "canceled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal canceledAmount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Version
    private long version;

    @Column(name = "tracking_carrier", length = 80)
    private String trackingCarrier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopOrder() {
    }

    public ShopOrder(
            UUID id,
            UUID requestId,
            String requestFingerprint,
            String orderNumber,
            UUID memberId,
            BigDecimal totalAmount,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            String usedCouponCode,
            BigDecimal balanceAfter,
            Instant now
    ) {
        this.id = id;
        this.requestId = requestId;
        this.requestFingerprint = requestFingerprint;
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.status = OrderStatus.PAID;
        this.fulfillmentStatus = FulfillmentStatus.PAID;
        this.totalAmount = totalAmount;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.usedCouponCode = normalizeCouponCode(usedCouponCode);
        this.canceledAmount = BigDecimal.ZERO;
        this.balanceAfter = balanceAfter;
        this.orderedAt = databaseTimestamp(now);
        this.updatedAt = databaseTimestamp(now);
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

    public boolean hasFingerprint(String fingerprint) {
        return requestFingerprint.equals(fingerprint);
    }

    public FulfillmentStatus fulfillmentStatus() { return fulfillmentStatus; }

    public String trackingCarrier() { return trackingCarrier; }

    public String trackingNumber() { return trackingNumber; }

    public String trackingUrl() { return trackingUrl; }

    public Instant estimatedDeliveryAt() { return estimatedDeliveryAt; }

    public BigDecimal originalAmount() { return originalAmount; }

    public BigDecimal discountAmount() { return discountAmount; }

    public String usedCouponCode() { return usedCouponCode; }

    public void transitionFulfillment(FulfillmentStatus next, Instant now) {
        if (!fulfillmentStatus.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "허용되지 않는 배송 상태 변경입니다.");
        }
        fulfillmentStatus = next;
        updatedAt = databaseTimestamp(now);
    }

    public void applyTracking(String carrier, String number, String url, Instant now) {
        this.trackingCarrier = normalizeTrackingField(carrier);
        this.trackingNumber = normalizeTrackingField(number);
        this.trackingUrl = normalizeTrackingField(url);
        this.updatedAt = databaseTimestamp(now);
    }

    public boolean isCancelable() { return fulfillmentStatus.isCancelable(); }

    public void applyCancellation(BigDecimal amount, boolean fullyCanceled, Instant now) {
        canceledAmount = canceledAmount.add(amount);
        status = fullyCanceled ? OrderStatus.CANCELED : OrderStatus.PARTIALLY_CANCELED;
        updatedAt = databaseTimestamp(now);
    }

    public OrderView toView(List<OrderItemView> items) {
        return new OrderView(
                id,
                orderNumber,
                status.name(),
                fulfillmentStatus.name(),
                totalAmount,
                canceledAmount,
                balanceAfter,
                orderedAt,
                items
        )
                .withTracking(trackingCarrier, trackingNumber, trackingUrl, estimatedDeliveryAt)
                .withDiscount(usedCouponCode, originalAmount, discountAmount);
    }

    public OrderView toCreationView(List<OrderItemView> items) {
        return new OrderView(
                id,
                orderNumber,
                OrderStatus.PAID.name(),
                FulfillmentStatus.PAID.name(),
                totalAmount,
                BigDecimal.ZERO,
                balanceAfter,
                orderedAt,
                items
        )
                .withTracking(trackingCarrier, trackingNumber, trackingUrl, estimatedDeliveryAt)
                .withDiscount(usedCouponCode, originalAmount, discountAmount);
    }

    private static String normalizeTrackingField(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeCouponCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase();
    }

    private static Instant databaseTimestamp(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.MICROS);
    }
}
