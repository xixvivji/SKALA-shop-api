package com.skala.shopping.order.internal;

import java.util.UUID;
import java.util.List;
import com.skala.shopping.order.OrderLineCommand;
import com.skala.shopping.order.ShippingAddressCommand;
import java.math.BigDecimal;

final class OrderCommandFingerprint {

    private static final String ORDER_OPERATION = "ORDER";
    private static final String CANCELLATION_OPERATION = "CANCEL";

    private OrderCommandFingerprint() {
    }

    static String order(UUID memberId, UUID productId, int quantity) {
        return order(memberId, List.of(new OrderLineCommand(productId, quantity)), null, null);
    }

    static String cancellation(UUID memberId, UUID productId, int quantity) {
        return build(CANCELLATION_OPERATION, memberId, productId, quantity);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address) {
        return order(memberId, items, address, null);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address, String couponCode) {
        return order(memberId, items, address, couponCode, null);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address,
                        String couponCode, BigDecimal pointAmount) {
        String lines = items.stream()
                .sorted(java.util.Comparator.comparing(line -> line.getVariantId().toString()))
                .map(line -> line.getProductId() + ":" + line.getVariantId() + ":" + line.getQuantity())
                .collect(java.util.stream.Collectors.joining(","));
        String shipping = address == null ? "" : String.join("|",
                address.getRecipientName(), address.getPhoneNumber(), address.getPostalCode(),
                address.getAddressLine1(), address.getAddressLine2() == null ? "" : address.getAddressLine2());
        String normalizedCoupon = couponCode == null ? "" : couponCode.trim().toUpperCase();
        String points = pointAmount == null ? "ALL" : pointAmount.stripTrailingZeros().toPlainString();
        return ORDER_OPERATION + "|" + memberId + "|" + lines + "|" + shipping + "|"
                + normalizedCoupon + "|" + points;
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
