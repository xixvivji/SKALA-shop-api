package com.skala.shopping.order.internal.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class FulfillmentStatusTests {
    @Test void allowsOnlyTheNextForwardTransition() {
        assertTrue(FulfillmentStatus.PAID.canTransitionTo(FulfillmentStatus.PREPARING));
        assertFalse(FulfillmentStatus.PAID.canTransitionTo(FulfillmentStatus.SHIPPED));
        assertFalse(FulfillmentStatus.SHIPPED.canTransitionTo(FulfillmentStatus.PREPARING));
        assertFalse(FulfillmentStatus.DELIVERED.canTransitionTo(FulfillmentStatus.DELIVERED));
    }
    @Test void allowsCancellationOnlyBeforeShipping() {
        assertTrue(FulfillmentStatus.PAID.isCancelable());
        assertTrue(FulfillmentStatus.PREPARING.isCancelable());
        assertFalse(FulfillmentStatus.SHIPPED.isCancelable());
        assertFalse(FulfillmentStatus.DELIVERED.isCancelable());
    }
}
