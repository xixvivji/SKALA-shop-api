package com.skala.shopping.outbox.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaOutboxMessagePublisherTests {
    @Test
    void publishesAggregateKeyAndPayloadToConfiguredTopic() {
        @SuppressWarnings("unchecked") KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        when(kafka.send("domain-events","order-1","{}"))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaOutboxMessagePublisher publisher=new KafkaOutboxMessagePublisher(
                kafka,"domain-events",Duration.ofSeconds(1));

        publisher.publish("order-1","OrderPlaced","{}");

        verify(kafka).send("domain-events","order-1","{}");
    }

    @Test
    void propagatesBrokerFailureSoOutboxCanRetry() {
        @SuppressWarnings("unchecked") KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        CompletableFuture failed=new CompletableFuture();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafka.send("domain-events","order-1","{}")) .thenReturn(failed);
        KafkaOutboxMessagePublisher publisher=new KafkaOutboxMessagePublisher(
                kafka,"domain-events",Duration.ofSeconds(1));

        assertThrows(IllegalStateException.class,
                () -> publisher.publish("order-1","OrderPlaced","{}"));
    }
}
