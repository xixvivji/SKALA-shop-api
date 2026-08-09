package com.skala.shopping.searchservice.messaging;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.skala.shopping.searchservice.service.ProductSearchService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
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
        "spring.elasticsearch.uris=http://127.0.0.1:1",
        "management.health.elasticsearch.enabled=false"
})
class KafkaEventIntegrationTests {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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
}
