package com.skala.shopping.coupon.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_usages", schema = "coupon")
public class CouponUsage {

    @Id
    private UUID id;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "coupon_code", nullable = false, length = 50)
    private String couponCode;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    protected CouponUsage() {
    }

    public CouponUsage(
            UUID couponId,
            String couponCode,
            UUID memberId,
            UUID orderId,
            UUID commandId,
            BigDecimal discountAmount,
            Instant usedAt
    ) {
        this.id = UUID.randomUUID();
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.memberId = memberId;
        this.orderId = orderId;
        this.commandId = commandId;
        this.discountAmount = discountAmount;
        this.usedAt = usedAt;
    }
}
