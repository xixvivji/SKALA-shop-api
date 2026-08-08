package com.skala.shopping.order.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    static String itemCancellation(UUID memberId, UUID orderItemId, int quantity) {
        return build(CANCELLATION_OPERATION + "_ITEM", memberId, orderItemId, quantity);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address) {
        return order(memberId, items, address, null);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address, String couponCode) {
        return order(memberId, items, address, couponCode, null);
    }

    static String order(UUID memberId, List<OrderLineCommand> items, ShippingAddressCommand address,
                        String couponCode, BigDecimal pointAmount) {
        return "v2:" + sha256(canonicalOrder(memberId, items, address, couponCode, pointAmount));
    }

    /** Builds the pre-v2 value so deployed orders remain replayable after the hash migration. */
    static String legacyOrder(UUID memberId, List<OrderLineCommand> items,
                              ShippingAddressCommand address, String couponCode,
                              BigDecimal pointAmount) {
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

    static boolean matchesOrder(String stored, String current, String legacy) {
        return stored != null && (stored.equals(current) || stored.equals(legacy));
    }

    private static String canonicalOrder(UUID memberId, List<OrderLineCommand> items,
                                         ShippingAddressCommand address, String couponCode,
                                         BigDecimal pointAmount) {
        StringBuilder value = new StringBuilder(ORDER_OPERATION);
        append(value, memberId.toString());
        items.stream()
                .sorted(java.util.Comparator.comparing(line -> line.getVariantId().toString()))
                .forEach(line -> {
                    append(value, line.getProductId().toString());
                    append(value, line.getVariantId().toString());
                    append(value, Integer.toString(line.getQuantity()));
                });
        if (address == null) {
            append(value, "NO_ADDRESS");
        } else {
            append(value, address.getRecipientName());
            append(value, address.getPhoneNumber());
            append(value, address.getPostalCode());
            append(value, address.getAddressLine1());
            append(value, address.getAddressLine2());
        }
        append(value, couponCode == null ? null : couponCode.trim().toUpperCase());
        append(value, pointAmount == null
                ? "ALL" : pointAmount.stripTrailingZeros().toPlainString());
        return value.toString();
    }

    private static void append(StringBuilder target, String value) {
        String normalized = value == null ? "" : value;
        target.append('|').append(normalized.length()).append(':').append(normalized);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
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
