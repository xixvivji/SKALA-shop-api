package com.skala.shopping.order.internal.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderItemTests {

    @Test
    void rejectsCancellationBeyondAvailableQuantityWithBusinessError() {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 상품",
                new BigDecimal("10000.00"),
                2
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> item.cancel(3)
        );

        assertEquals(ErrorCode.INSUFFICIENT_QUANTITY, exception.errorCode());
    }

    @Test
    void refundsOnlyTheDiscountedPaidAmountAcrossPartialCancellations() {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "할인 상품",
                new BigDecimal("10.00"),
                3,
                new BigDecimal("10.00"),
                0
        );

        assertEquals(new BigDecimal("3.33"), item.cancel(1));
        assertEquals(new BigDecimal("3.33"), item.cancel(1));
        assertEquals(new BigDecimal("3.34"), item.cancel(1));
        assertEquals(new BigDecimal("10.00"), item.toView().getRefundedAmount());
    }
}
