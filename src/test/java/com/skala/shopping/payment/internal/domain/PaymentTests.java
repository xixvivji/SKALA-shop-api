package com.skala.shopping.payment.internal.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTests {

    @Test
    void replaysFailedApprovalAndRejectsChangedRequestForTheSameKey() {
        Payment payment = payment();
        UUID commandId = UUID.randomUUID();
        payment.beginApproval(commandId, "v1:first", "4000-****-****-9995", Instant.now());
        payment.fail("CARD_DECLINED", "declined", Instant.now());
        payment.captureApprovalResult();

        assertTrue(payment.isApprovalReplay(commandId, "v1:first"));
        BusinessException conflict = assertThrows(
                BusinessException.class,
                () -> payment.isApprovalReplay(commandId, "v1:changed")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                ErrorCode.IDEMPOTENCY_CONFLICT, conflict.errorCode());
    }

    @Test
    void replaysSuccessfulApprovalWithoutChargingAgain() {
        Payment payment = payment();
        UUID commandId = UUID.randomUUID();
        payment.beginApproval(commandId, "v1:success", "4242-****-****-4242", Instant.now());
        payment.approve("fake-transaction", Instant.now());
        payment.captureApprovalResult();
        payment.refund(new BigDecimal("10000.00"), Instant.now());

        assertTrue(payment.isApprovalReplay(commandId, "v1:success"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "PAID", payment.approvalReplayView().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                BigDecimal.ZERO.setScale(2),
                payment.approvalReplayView().getRefundedAmount());
    }

    private Payment payment() {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "prepare",
                "CARD",
                new BigDecimal("10000.00"),
                Instant.now()
        );
    }
}
