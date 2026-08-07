package com.skala.shopping.coupon;

import java.math.BigDecimal;
import java.util.UUID;

public interface CouponApi {

    CouponDiscount preview(UUID memberId, String couponCode, BigDecimal orderAmount);

    void apply(
            UUID memberId,
            UUID orderId,
            UUID commandId,
            CouponDiscount discount
    );
}
