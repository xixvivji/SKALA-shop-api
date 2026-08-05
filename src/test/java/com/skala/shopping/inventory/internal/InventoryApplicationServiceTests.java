package com.skala.shopping.inventory.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.skala.shopping.inventory.StockBalance;
import com.skala.shopping.inventory.internal.domain.StockMovement;
import com.skala.shopping.inventory.internal.domain.StockMovementType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTests {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMovementRepository movementRepository;

    private InventoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InventoryApplicationService(stockRepository, movementRepository);
    }

    @Test
    void replaysReleaseFromPersistedSnapshotWithoutReadingCurrentStock() {
        UUID productId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        String fingerprint = "RELEASE|" + productId + "|3|";
        StockMovement movement = new StockMovement(
                operationId,
                productId,
                StockMovementType.RELEASE,
                3,
                9,
                false,
                fingerprint,
                null,
                Instant.parse("2026-08-05T00:00:00Z")
        );
        when(movementRepository.findByOperationIdAndProductId(operationId, productId))
                .thenReturn(Optional.of(movement));

        StockBalance replay = service.release(productId, 3, operationId);

        assertEquals(9, replay.getAvailableQuantity());
        assertEquals(0, replay.getMaxOrderQuantity());
        assertEquals(false, replay.isOrderable());
        assertEquals("INACTIVE", replay.getStockStatus());
        verifyNoInteractions(stockRepository);
    }
}
