package com.skala.shopping.outbox.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaOutboxMessagePublisherTests {

    @Test
    void publishesAggregateKeyPayloadAndEventTypeHeader() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafka, "domain-events", Duration.ofSeconds(1));

        publisher.publish("product-1", "ProductSearchChanged", "{\"id\":\"product-1\"}");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();
        assertEquals("domain-events", record.topic());
        assertEquals("product-1", record.key());
        assertEquals("{\"id\":\"product-1\"}", record.value());
        assertEquals(
                "ProductSearchChanged",
                new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8)
        );
    }

    @Test
    void propagatesBrokerFailureSoOutboxCanRetry() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        CompletableFuture failed = new CompletableFuture();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(failed);
        KafkaOutboxMessagePublisher publisher = new KafkaOutboxMessagePublisher(
                kafka, "domain-events", Duration.ofSeconds(1));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish("order-1", "OrderPlaced", "{}"));
    }
}
