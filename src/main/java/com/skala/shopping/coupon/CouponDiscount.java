package com.skala.shopping.coupon;

import java.math.BigDecimal;
import java.util.UUID;

public final class CouponDiscount {

    private final UUID couponId;
    private final String couponCode;
    private final BigDecimal discountAmount;

    public CouponDiscount(UUID couponId, String couponCode, BigDecimal discountAmount) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    public static CouponDiscount none(String couponCode) {
        return new CouponDiscount(null, normalizeCode(couponCode), BigDecimal.ZERO);
    }

    public UUID getCouponId() {
        return couponId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public UUID couponId() {
        return couponId;
    }

    public String couponCode() {
        return couponCode;
    }

    public BigDecimal discountAmount() {
        return discountAmount;
    }

    private static String normalizeCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }
}
