package com.skala.shopping.searchservice.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.searchservice.service.ProductSearchService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class ProductSearchEventConsumerTests {

    private static final String PRODUCT_EVENT =
            "com.skala.shopping.catalog.ProductSearchChanged";

    @Test
    void consumesOnlyProductSearchChangedEvent() {
        ProductSearchService service = mock(ProductSearchService.class);
        ProductSearchEventConsumer consumer = new ProductSearchEventConsumer(
                new ObjectMapper(), service, PRODUCT_EVENT);
        UUID id = UUID.randomUUID();
        ConsumerRecord<String, String> record = record(
                PRODUCT_EVENT,
                "{\"id\":\"" + id + "\",\"name\":\"Kafka 상품\",\"price\":1000,\"deleted\":false}"
        );

        consumer.consume(record);

        verify(service).apply(org.mockito.ArgumentMatchers.argThat(event -> id.equals(event.getId())));
    }

    @Test
    void ignoresUnrelatedDomainEventOnSharedTopic() {
        ProductSearchService service = mock(ProductSearchService.class);
        ProductSearchEventConsumer consumer = new ProductSearchEventConsumer(
                new ObjectMapper(), service, PRODUCT_EVENT);

        consumer.consume(record("com.skala.shopping.order.OrderPlaced", "{}"));

        verify(service, never()).apply(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidProductEventFailsSoKafkaErrorHandlerCanRetryAndUseDlt() {
        ProductSearchEventConsumer consumer = new ProductSearchEventConsumer(
                new ObjectMapper(), mock(ProductSearchService.class), PRODUCT_EVENT);

        assertThrows(IllegalArgumentException.class,
                () -> consumer.consume(record(PRODUCT_EVENT, "not-json")));
    }

    private ConsumerRecord<String, String> record(String eventType, String value) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "skala-shop.domain-events", 0, 3L, "product-1", value);
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
