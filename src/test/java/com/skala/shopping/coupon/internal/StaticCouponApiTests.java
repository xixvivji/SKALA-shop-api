package com.skala.shopping.coupon.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.coupon.CouponDiscount;
import com.skala.shopping.coupon.internal.domain.CouponUsage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaticCouponApiTests {

    private static final Instant NOW = Instant.parse("2026-08-07T00:00:00Z");

    @Mock
    private CouponUsageRepository repository;

    private StaticCouponApi couponApi;

    @BeforeEach
    void setUp() {
        couponApi = new StaticCouponApi(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsCouponAlreadyUsedByMember() {
        UUID memberId = UUID.randomUUID();
        when(repository.existsByMemberIdAndCouponCode(memberId, "WELCOME10")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> couponApi.preview(memberId, "welcome10", new BigDecimal("10000.00"))
        );

        assertEquals(ErrorCode.DATA_DUPLICATED, exception.errorCode());
    }

    @Test
    void persistsAppliedCouponUsage() {
        UUID memberId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        CouponDiscount discount = couponApi.preview(
                memberId,
                "SAVE5000",
                new BigDecimal("10000.00")
        );

        couponApi.apply(memberId, orderId, commandId, discount);

        verify(repository).save(org.mockito.ArgumentMatchers.any(CouponUsage.class));
        assertEquals(new BigDecimal("5000.00"), discount.getDiscountAmount());
    }
}
