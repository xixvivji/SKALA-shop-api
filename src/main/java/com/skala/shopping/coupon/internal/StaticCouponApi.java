package com.skala.shopping.coupon.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.coupon.CouponApi;
import com.skala.shopping.coupon.CouponDiscount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class StaticCouponApi implements CouponApi {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final int MONEY_SCALE = 2;

    private static final Map<String, Rule> COUPONS = Map.of(
            "WELCOME10", Rule.percent("WELCOME10", new BigDecimal("0.10")),
            "NEWBIE5", Rule.percent("NEWBIE5", new BigDecimal("0.05")),
            "SAVE5000", Rule.fixed("SAVE5000", new BigDecimal("5000")),
            "SAVE10000", Rule.fixed("SAVE10000", new BigDecimal("10000"))
    );

    @Override
    public CouponDiscount preview(UUID memberId, String couponCode, BigDecimal orderAmount) {
        String normalizedCode = normalizeCode(couponCode);
        if (normalizedCode == null) {
            return CouponDiscount.none(null);
        }
        Rule rule = COUPONS.get(normalizedCode);
        if (rule == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 쿠폰 코드입니다.");
        }
        if (orderAmount == null || orderAmount.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "주문 금액이 유효하지 않습니다.");
        }
        BigDecimal normalizedAmount = orderAmount.setScale(MONEY_SCALE, RoundingMode.DOWN);
        BigDecimal discount = rule.fixedAmount == null
                ? normalizedAmount.multiply(rule.percent)
                .setScale(MONEY_SCALE, RoundingMode.DOWN)
                : rule.fixedAmount;
        if (discount.compareTo(normalizedAmount) > 0) {
            discount = normalizedAmount;
        }
        if (discount.signum() <= 0) {
            return CouponDiscount.none(normalizedCode);
        }
        return new CouponDiscount(couponId(normalizedCode), normalizedCode, discount);
    }

    @Override
    public void apply(
            UUID memberId,
            UUID orderId,
            UUID commandId,
            CouponDiscount discount
    ) {
        // no-op: 실제 정산형 쿠폰 사용 이력 저장이 필요한 경우 이곳에 누적 기록을 추가
    }

    private static String normalizeCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase();
    }

    private static UUID couponId(String normalizedCode) {
        return UUID.nameUUIDFromBytes(("COUPON:" + normalizedCode)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static final class Rule {
        private final String code;
        private final BigDecimal percent;
        private final BigDecimal fixedAmount;

        private Rule(String code, BigDecimal percent, BigDecimal fixedAmount) {
            this.code = code;
            this.percent = percent;
            this.fixedAmount = fixedAmount;
        }

        private static Rule percent(String code, BigDecimal percent) {
            return new Rule(code, percent, null);
        }

        private static Rule fixed(String code, BigDecimal amount) {
            return new Rule(code, BigDecimal.ZERO, amount);
        }
    }
}
