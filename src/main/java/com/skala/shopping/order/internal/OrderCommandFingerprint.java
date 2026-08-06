package com.skala.shopping.order.internal;

import java.util.UUID;
import java.util.List;
import com.skala.shopping.order.OrderLineCommand;
import com.skala.shopping.order.ShippingAddressCommand;

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

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address) {
        String lines = items.stream()
                .sorted(java.util.Comparator.comparing(line -> line.getProductId().toString()))
                .map(line -> line.getProductId() + ":" + line.getQuantity())
                .collect(java.util.stream.Collectors.joining(","));
        String shipping = address == null ? "" : String.join("|",
                address.getRecipientName(), address.getPhoneNumber(), address.getPostalCode(),
                address.getAddressLine1(), address.getAddressLine2() == null ? "" : address.getAddressLine2());
        return ORDER_OPERATION + "|" + memberId + "|" + lines + "|" + shipping;
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
