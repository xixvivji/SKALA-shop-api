package com.skala.shopping.order.internal;

import com.skala.shopping.coupon.CouponApi;
import com.skala.shopping.coupon.CouponDiscount;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalCouponManager implements CouponManager {

    private final CouponApi couponApi;

    LocalCouponManager(CouponApi couponApi) {
        this.couponApi = couponApi;
    }

    @Override
    public CouponDiscount applyPreview(UUID memberId, String couponCode, BigDecimal orderAmount) {
        return couponApi.preview(memberId, couponCode, orderAmount);
    }

    @Override
    public void applyUsage(UUID memberId, UUID orderId, UUID commandId, CouponDiscount discount) {
        couponApi.apply(memberId, orderId, commandId, discount);
    }
}
