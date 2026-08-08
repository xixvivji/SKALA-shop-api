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

    @Column(name = "point_used_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal pointUsedAmount;

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentAmount;

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
            BigDecimal balanceAfter,
            Instant now
    ) {
        this(
                id,
                requestId,
                requestFingerprint,
                orderNumber,
                memberId,
                totalAmount,
                totalAmount,
                BigDecimal.ZERO,
                null,
                totalAmount,
                BigDecimal.ZERO,
                balanceAfter,
                now
        );
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
            BigDecimal pointUsedAmount,
            BigDecimal paymentAmount,
            BigDecimal balanceAfter,
            Instant now
    ) {
        this.id = id;
        this.requestId = requestId;
        this.requestFingerprint = requestFingerprint;
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.status = paymentAmount.signum() == 0
                ? OrderStatus.PAID : OrderStatus.PAYMENT_PENDING;
        this.fulfillmentStatus = paymentAmount.signum() == 0
                ? FulfillmentStatus.PAID : FulfillmentStatus.PAYMENT_PENDING;
        this.totalAmount = totalAmount;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.usedCouponCode = normalizeCouponCode(usedCouponCode);
        this.canceledAmount = BigDecimal.ZERO;
        this.balanceAfter = balanceAfter;
        this.pointUsedAmount = pointUsedAmount;
        this.paymentAmount = paymentAmount;
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

    public String requestFingerprint() { return requestFingerprint; }

    public FulfillmentStatus fulfillmentStatus() { return fulfillmentStatus; }

    public String trackingCarrier() { return trackingCarrier; }

    public String trackingNumber() { return trackingNumber; }

    public String trackingUrl() { return trackingUrl; }

    public Instant estimatedDeliveryAt() { return estimatedDeliveryAt; }

    public BigDecimal originalAmount() { return originalAmount; }

    public BigDecimal discountAmount() { return discountAmount; }

    public String usedCouponCode() { return usedCouponCode; }

    public UUID requestId() { return requestId; }

    public BigDecimal totalAmount() { return totalAmount; }

    public BigDecimal pointUsedAmount() { return pointUsedAmount; }

    public BigDecimal paymentAmount() { return paymentAmount; }

    public BigDecimal canceledAmount() { return canceledAmount; }

    public boolean isPaymentPending() { return status == OrderStatus.PAYMENT_PENDING; }

    public void confirmPayment(Instant now) {
        if (status == OrderStatus.PAID) return;
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "결제 대기 주문만 승인할 수 있습니다.");
        }
        status = OrderStatus.PAID;
        fulfillmentStatus = FulfillmentStatus.PAID;
        updatedAt = databaseTimestamp(now);
    }

    public void failPayment(BigDecimal restoredBalance, Instant now) {
        if (status == OrderStatus.PAYMENT_FAILED) return;
        if (status != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "결제 대기 주문만 실패 처리할 수 있습니다.");
        }
        status = OrderStatus.PAYMENT_FAILED;
        balanceAfter = restoredBalance;
        updatedAt = databaseTimestamp(now);
    }

    public void transitionFulfillment(FulfillmentStatus next, Instant now) {
        requireActiveFulfillment();
        if (!fulfillmentStatus.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "허용되지 않는 배송 상태 변경입니다.");
        }
        fulfillmentStatus = next;
        updatedAt = databaseTimestamp(now);
    }

    public void applyTracking(
            String carrier,
            String number,
            String url,
            Instant estimatedDeliveryAt,
            Instant now
    ) {
        requireActiveFulfillment();
        if (carrier != null) {
            this.trackingCarrier = normalizeTrackingField(carrier);
        }
        if (number != null) {
            this.trackingNumber = normalizeTrackingField(number);
        }
        if (url != null) {
            this.trackingUrl = normalizeTrackingField(url);
        }
        if (estimatedDeliveryAt != null) {
            this.estimatedDeliveryAt = databaseTimestamp(estimatedDeliveryAt);
        }
        this.updatedAt = databaseTimestamp(now);
    }

    public boolean isCancelable() {
        return (status == OrderStatus.PAID || status == OrderStatus.PARTIALLY_CANCELED)
                && fulfillmentStatus.isCancelable();
    }

    public void applyCancellation(BigDecimal amount, boolean fullyCanceled, Instant now) {
        canceledAmount = canceledAmount.add(amount);
        status = fullyCanceled ? OrderStatus.CANCELED : OrderStatus.PARTIALLY_CANCELED;
        updatedAt = databaseTimestamp(now);
    }

    private void requireActiveFulfillment() {
        if (status != OrderStatus.PAID && status != OrderStatus.PARTIALLY_CANCELED) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "결제가 완료된 활성 주문만 배송 상태나 운송장 정보를 변경할 수 있습니다."
            );
        }
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
                .withCoupon(usedCouponCode, originalAmount, discountAmount)
                .withPayment(pointUsedAmount, paymentAmount);
    }

    public OrderView toCreationView(List<OrderItemView> items) {
        return new OrderView(
                id,
                orderNumber,
                paymentAmount.signum() == 0
                        ? OrderStatus.PAID.name() : OrderStatus.PAYMENT_PENDING.name(),
                paymentAmount.signum() == 0
                        ? FulfillmentStatus.PAID.name() : FulfillmentStatus.PAYMENT_PENDING.name(),
                totalAmount,
                BigDecimal.ZERO,
                balanceAfter,
                orderedAt,
                items
        )
                .withTracking(trackingCarrier, trackingNumber, trackingUrl, estimatedDeliveryAt)
                .withCoupon(usedCouponCode, originalAmount, discountAmount)
                .withPayment(pointUsedAmount, paymentAmount);
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
