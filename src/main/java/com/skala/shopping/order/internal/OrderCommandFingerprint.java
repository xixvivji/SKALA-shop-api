package com.skala.shopping.order.internal;

import java.util.UUID;

final class OrderCommandFingerprint {

    private static final String ORDER_OPERATION = "ORDER";
    private static final String CANCELLATION_OPERATION = "CANCEL";

    private OrderCommandFingerprint() {
    }

    static String order(UUID memberId, UUID productId, int quantity) {
        return build(ORDER_OPERATION, memberId, productId, quantity);
    }

    static String cancellation(UUID memberId, UUID productId, int quantity) {
        return build(CANCELLATION_OPERATION, memberId, productId, quantity);
    }

    private static String build(
            String operation,
            UUID memberId,
            UUID productId,
            int quantity
    ) {
        return operation + "|" + memberId + "|" + productId + "|" + quantity;
    }
}
