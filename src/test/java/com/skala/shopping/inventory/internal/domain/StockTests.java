package com.skala.shopping.inventory.internal.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.inventory.StockBalance;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockTests {

    private static final Instant NOW = Instant.parse("2026-08-05T00:00:00Z");

    @Test
    void reservesAndReleasesAvailableQuantity() {
        Stock stock = new Stock(UUID.randomUUID(), 5, NOW);

        stock.reserve(3, NOW.plusSeconds(1));
        assertEquals(2, stock.toBalance().getAvailableQuantity());

        stock.release(1, NOW.plusSeconds(2));
        assertEquals(3, stock.toBalance().getAvailableQuantity());
    }

    @Test
    void rejectsReservationOverAvailableQuantity() {
        Stock stock = new Stock(UUID.randomUUID(), 1, NOW);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> stock.reserve(2, NOW)
        );

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.errorCode());
        assertEquals(1, stock.toBalance().getAvailableQuantity());
    }

    @Test
    void rejectsNonPositiveMovementQuantity() {
        Stock stock = new Stock(UUID.randomUUID(), 1, NOW);

        assertEquals(
                ErrorCode.INVALID_PARAMETER,
                assertThrows(BusinessException.class, () -> stock.reserve(0, NOW)).errorCode()
        );
        assertEquals(
                ErrorCode.INVALID_PARAMETER,
                assertThrows(BusinessException.class, () -> stock.release(-1, NOW)).errorCode()
        );
        assertEquals(1, stock.toBalance().getAvailableQuantity());
    }

    @Test
    void makesDeactivatedStockUnorderableButAllowsCancellationRelease() {
        Stock stock = new Stock(UUID.randomUUID(), 1, NOW);

        stock.deactivate(NOW.plusSeconds(1));

        assertEquals("INACTIVE", stock.toBalance().getStockStatus());
        assertEquals(false, stock.toBalance().isOrderable());
        assertEquals(0, stock.toBalance().getMaxOrderQuantity());
        assertEquals(
                ErrorCode.PRODUCT_NOT_SALEABLE,
                assertThrows(BusinessException.class, () -> stock.reserve(1, NOW)).errorCode()
        );

        stock.release(1, NOW.plusSeconds(2));
        assertEquals(2, stock.toBalance().getAvailableQuantity());
        assertEquals("INACTIVE", stock.toBalance().getStockStatus());
    }

    @Test
    void capsMaximumOrderQuantityAtApiPolicyLimit() {
        StockBalance stock = new StockBalance(UUID.randomUUID(), 2_000_000, true);

        assertEquals(1_000_000, stock.getMaxOrderQuantity());
    }
}
