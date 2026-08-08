package com.skala.shopping.payment.internal;

import java.math.BigDecimal;
import java.util.UUID;

interface FakeGateway {
    FakeGatewayResult approve(UUID paymentId, BigDecimal amount, String testCardNumber);
    void cancel(String providerTransactionId, BigDecimal amount);
}
