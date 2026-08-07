package com.skala.shopping.payment;

import com.skala.shopping.common.PageResponse;
import java.util.UUID;
import java.math.BigDecimal;

public interface PaymentApi {
    PaymentView prepare(UUID memberId, UUID orderId, String method, UUID commandId);
    PaymentView approve(UUID memberId, UUID paymentId, String testCardNumber, UUID commandId);
    PaymentView get(UUID memberId, UUID paymentId);
    PaymentView getByOrder(UUID memberId, UUID orderId);
    PageResponse<PaymentView> getMine(UUID memberId, int page, int size);
    PageResponse<PaymentView> getAll(int page, int size);
    PaymentView refund(UUID paymentId, BigDecimal amount, UUID commandId);
    PaymentView reconcile(UUID paymentId);
    PaymentView processWebhook(UUID eventId, UUID paymentId, String eventType);
}
