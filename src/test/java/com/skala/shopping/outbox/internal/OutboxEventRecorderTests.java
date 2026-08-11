package com.skala.shopping.outbox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.stockalert.StockAlertTriggered;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OutboxEventRecorderTests {

    @Test
    void recordsProductSearchChangedInTheCatalogTransaction() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventRecorder recorder = new OutboxEventRecorder(repository, new ObjectMapper());
        UUID productId = UUID.randomUUID();
        ProductSearchChanged event = new ProductSearchChanged(
                new ProductSnapshot(productId, "Kafka 검색 상품", new BigDecimal("10000"), "ACTIVE"),
                false
        );

        recorder.on(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertEquals(productId, captor.getValue().aggregateId());
        assertEquals(ProductSearchChanged.class.getName(), captor.getValue().eventType());
    }

    @Test
    void recordsStockAlertTriggeredForTheNotificationConsumer() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventRecorder recorder = new OutboxEventRecorder(
                repository,
                new ObjectMapper().findAndRegisterModules()
        );
        UUID subscriptionId = UUID.randomUUID();
        StockAlertTriggered event = new StockAlertTriggered(
                subscriptionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                7,
                Instant.parse("2026-08-11T00:00:00Z")
        );

        recorder.on(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("STOCK_ALERT", captor.getValue().aggregateType());
        assertEquals(subscriptionId, captor.getValue().aggregateId());
        assertEquals(StockAlertTriggered.class.getName(), captor.getValue().eventType());
    }
}
