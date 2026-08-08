package com.skala.shopping.payment.internal;

import com.skala.shopping.order.OrderPaymentRefundRequested;
import com.skala.shopping.payment.PaymentApi;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Applies an order cancellation to the external-payment ledger in the same transaction. */
@Component
class OrderPaymentRefundListener {

    private final PaymentApi paymentApi;

    OrderPaymentRefundListener(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    @EventListener
    void on(OrderPaymentRefundRequested event) {
        paymentApi.refundByOrder(
                event.getMemberId(),
                event.getOrderId(),
                event.getAmount(),
                event.getCommandId()
        );
    }
}
