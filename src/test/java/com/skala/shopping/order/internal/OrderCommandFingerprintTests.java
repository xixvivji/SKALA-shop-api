package com.skala.shopping.order.internal;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skala.shopping.order.OrderLineCommand;
import com.skala.shopping.order.ShippingAddressCommand;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderCommandFingerprintTests {

    @Test
    void hashesMaximumOrderWithinThePersistedColumnLimit() {
        List<OrderLineCommand> lines = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            lines.add(new OrderLineCommand(UUID.randomUUID(), UUID.randomUUID(), 1));
        }
        ShippingAddressCommand address = new ShippingAddressCommand(
                "받는 사람", "010-1234-5678", "12345", "서울시 테스트로 1", "상세 주소");

        String fingerprint = OrderCommandFingerprint.order(
                UUID.randomUUID(), lines, address, "SAVE5000", new BigDecimal("1000.00"));

        assertTrue(fingerprint.length() <= 2048);
        assertTrue(fingerprint.startsWith("v2:"));
    }

    @Test
    void lengthPrefixPreventsShippingDelimiterCollisions() {
        UUID memberId = UUID.randomUUID();
        List<OrderLineCommand> lines = List.of(
                new OrderLineCommand(UUID.randomUUID(), UUID.randomUUID(), 1));
        ShippingAddressCommand first = new ShippingAddressCommand(
                "A|B", "C", "D", "E", "F");
        ShippingAddressCommand second = new ShippingAddressCommand(
                "A", "B|C", "D", "E", "F");

        assertNotEquals(
                OrderCommandFingerprint.order(memberId, lines, first, null, null),
                OrderCommandFingerprint.order(memberId, lines, second, null, null)
        );
    }

    @Test
    void acceptsTheLegacyRawFingerprintDuringReplay() {
        UUID memberId = UUID.randomUUID();
        List<OrderLineCommand> lines = List.of(
                new OrderLineCommand(UUID.randomUUID(), UUID.randomUUID(), 1));
        String current = OrderCommandFingerprint.order(memberId, lines, null, null, null);
        String legacy = OrderCommandFingerprint.legacyOrder(memberId, lines, null, null, null);

        assertTrue(OrderCommandFingerprint.matchesOrder(legacy, current, legacy));
    }
}
