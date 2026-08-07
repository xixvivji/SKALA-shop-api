package com.skala.shopping.order.internal;

import com.skala.shopping.coupon.CouponDiscount;
import java.math.BigDecimal;
import java.util.UUID;

interface CouponManager {

    CouponDiscount applyPreview(UUID memberId, String couponCode, BigDecimal orderAmount);

    void applyUsage(UUID memberId, UUID orderId, UUID commandId, CouponDiscount discount);
}
