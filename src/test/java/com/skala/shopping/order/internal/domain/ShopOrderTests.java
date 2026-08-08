package com.skala.shopping.order.internal.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShopOrderTests {

    @Test
    void normalizesResponseTimestampToPostgresqlMicrosecondPrecision() {
        Instant nanosecondTimestamp = Instant.parse("2026-08-05T13:09:53.123456789Z");
        ShopOrder order = new ShopOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "fingerprint",
                "SKALA-20260805-123456789ABC",
                UUID.randomUUID(),
                new BigDecimal("30000.00"),
                new BigDecimal("970000.00"),
                nanosecondTimestamp
        );

        assertEquals(
                Instant.parse("2026-08-05T13:09:53.123456Z"),
                order.orderedAt()
        );
        assertEquals(order.orderedAt(), order.toCreationView(List.of()).getOrderedAt());
    }

    @Test
    void rejectsInvalidFulfillmentTransitionWithBusinessError() {
        ShopOrder order = new ShopOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "fingerprint",
                "SKALA-20260805-123456789ABC",
                UUID.randomUUID(),
                new BigDecimal("30000.00"),
                new BigDecimal("970000.00"),
                Instant.parse("2026-08-05T13:09:53Z")
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> order.transitionFulfillment(
                        FulfillmentStatus.SHIPPED,
                        Instant.parse("2026-08-05T13:10:53Z")
                )
        );

        assertEquals(ErrorCode.INVALID_PARAMETER, exception.errorCode());
    }

    @Test
    void blocksFulfillmentAndTrackingAfterFullCancellation() {
        ShopOrder order = new ShopOrder(
                UUID.randomUUID(), UUID.randomUUID(), "fingerprint",
                "SKALA-20260805-ABCDEF123456", UUID.randomUUID(),
                new BigDecimal("10000.00"), new BigDecimal("990000.00"), Instant.now());
        order.applyCancellation(new BigDecimal("10000.00"), true, Instant.now());

        assertThrows(BusinessException.class,
                () -> order.transitionFulfillment(FulfillmentStatus.PREPARING, Instant.now()));
        assertThrows(BusinessException.class,
                () -> order.applyTracking("택배", "TRACK-1", null, null, Instant.now()));
    }

    @Test
    void blocksFulfillmentWhileExternalPaymentIsPending() {
        ShopOrder order = new ShopOrder(
                UUID.randomUUID(), UUID.randomUUID(), "fingerprint",
                "SKALA-20260805-ABCDEF654321", UUID.randomUUID(),
                new BigDecimal("10000.00"), new BigDecimal("10000.00"),
                BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("10000.00"),
                new BigDecimal("1000000.00"), Instant.now());

        assertThrows(BusinessException.class,
                () -> order.transitionFulfillment(FulfillmentStatus.PAID, Instant.now()));
        assertThrows(BusinessException.class,
                () -> order.applyTracking("택배", "TRACK-2", null, null, Instant.now()));
    }
}
