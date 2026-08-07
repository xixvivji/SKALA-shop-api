package com.skala.shopping.inventory.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.inventory.StockBalance;
import com.skala.shopping.inventory.StockReplenished;
import com.skala.shopping.inventory.internal.domain.Stock;
import com.skala.shopping.inventory.internal.domain.StockMovement;
import com.skala.shopping.inventory.internal.domain.StockMovementType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTests {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InventoryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InventoryApplicationService(stockRepository, movementRepository, eventPublisher);
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

    @Test
    void publishesReplenishedEventWhenStockChangesFromZeroToPositive() {
        UUID productId = UUID.randomUUID();
        Stock stock = new Stock(productId, 0, Instant.parse("2026-08-05T00:00:00Z"));
        when(stockRepository.findByProductIdForUpdate(productId)).thenReturn(Optional.of(stock));

        service.release(productId, 4, UUID.randomUUID());

        ArgumentCaptor<StockReplenished> event = ArgumentCaptor.forClass(StockReplenished.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(productId, event.getValue().getProductId());
        assertEquals(4, event.getValue().getAvailableQuantity());
    }
}
