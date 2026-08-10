package com.skala.shopping.searchservice.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.skala.shopping.searchservice.service.ProductSearchService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EmbeddedKafka(
        partitions = 1,
        topics = {"search-integration-events", "search-integration-events.DLT"}
)
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "shopping.search.kafka-topic=search-integration-events",
        "shopping.search.product-event-type=com.skala.shopping.catalog.ProductSearchChanged",
        "shopping.search.retry-interval=10ms",
        "shopping.search.retry-attempts=2",
        "spring.elasticsearch.uris=http://127.0.0.1:1",
        "management.health.elasticsearch.enabled=false"
})
class KafkaEventIntegrationTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    private ProductSearchService searchService;

    @Test
    void consumesBackendOutboxRecordThroughRealKafkaBroker() throws Exception {
        UUID productId = UUID.randomUUID();
        ProducerRecord<String, String> record = new ProducerRecord<>(
                "search-integration-events",
                productId.toString(),
                "{\"id\":\"" + productId
                        + "\",\"name\":\"Kafka 통합 상품\",\"price\":19000,\"deleted\":false}"
        );
        record.headers().add(
                "eventType",
                "com.skala.shopping.catalog.ProductSearchChanged".getBytes(StandardCharsets.UTF_8)
        );

        kafkaTemplate.send(record).get();

        verify(searchService, timeout(10_000)).apply(argThat(event ->
                productId.equals(event.getId()) && "Kafka 통합 상품".equals(event.getName())));
    }

    @Test
    void sendsInvalidProductEventToDltAfterConfiguredRetries() throws Exception {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                "search-dlt-verifier", "true", embeddedKafka);
        try (Consumer<String, String> dltConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(
                    dltConsumer,
                    "search-integration-events.DLT"
            );
            ProducerRecord<String, String> invalid = new ProducerRecord<>(
                    "search-integration-events",
                    "invalid-product",
                    "not-json"
            );
            invalid.headers().add(
                    "eventType",
                    "com.skala.shopping.catalog.ProductSearchChanged"
                            .getBytes(StandardCharsets.UTF_8)
            );

            kafkaTemplate.send(invalid).get();

            ConsumerRecord<String, String> deadLetter = KafkaTestUtils.getSingleRecord(
                    dltConsumer,
                    "search-integration-events.DLT",
                    Duration.ofSeconds(10)
            );
            assertEquals("invalid-product", deadLetter.key());
            assertEquals("not-json", deadLetter.value());
            verify(searchService, never()).apply(org.mockito.ArgumentMatchers.any());
        }
    }
}
