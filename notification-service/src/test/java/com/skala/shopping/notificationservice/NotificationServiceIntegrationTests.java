package com.skala.shopping.notificationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skala.shopping.notificationservice.domain.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {"notification-integration-events", "notification-integration-events.DLT"}
)
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "shopping.notification.kafka-topic=notification-integration-events",
        "shopping.notification.retry-interval=10ms",
        "shopping.notification.retry-attempts=2",
        "management.health.elasticsearch.enabled=false"
})
class NotificationServiceIntegrationTests {

    private static final String ORDER_PLACED = "com.skala.shopping.order.OrderPlaced";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
        jdbcTemplate.update("DELETE FROM consumed_events");
    }

    @Test
    void storesOneNotificationWhenKafkaRedeliversTheSameOrderEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String payload = """
                {
                  "orderId":"%s",
                  "memberId":"%s",
                  "totalAmount":35000,
                  "occurredAt":"2026-08-11T00:00:00Z"
                }
                """.formatted(orderId, memberId);

        send(ORDER_PLACED, orderId.toString(), payload);
        send(ORDER_PLACED, orderId.toString(), payload);

        awaitNotificationCount(1);
        assertThat(repository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getMemberId()).isEqualTo(memberId);
            assertThat(notification.getReferenceId()).isEqualTo(orderId);
            assertThat(notification.getMessage()).contains("35000원");
        });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM consumed_events",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void sendsMalformedSupportedEventToDlt() throws Exception {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(
                "notification-dlt-verifier-" + UUID.randomUUID(),
                "true",
                embeddedKafka
        );
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(
                    consumer,
                    "notification-integration-events.DLT"
            );
            send(ORDER_PLACED, "invalid-order", "not-json");

            ConsumerRecord<String, String> deadLetter = KafkaTestUtils.getSingleRecord(
                    consumer,
                    "notification-integration-events.DLT",
                    Duration.ofSeconds(10)
            );
            assertThat(deadLetter.key()).isEqualTo("invalid-order");
            assertThat(deadLetter.value()).isEqualTo("not-json");
        }
    }

    @Test
    void exposesOnlyTheAuthenticatedMembersNotificationsAndMarksThemRead() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID anotherMemberId = UUID.randomUUID();
        UUID ownOrderId = UUID.randomUUID();
        UUID otherOrderId = UUID.randomUUID();
        send(ORDER_PLACED, ownOrderId.toString(), orderPayload(ownOrderId, memberId));
        send(ORDER_PLACED, otherOrderId.toString(), orderPayload(otherOrderId, anotherMemberId));
        awaitNotificationCount(2);

        UUID ownNotificationId = repository.findAll().stream()
                .filter(notification -> memberId.equals(notification.getMemberId()))
                .findFirst()
                .orElseThrow()
                .getId();
        UUID otherNotificationId = repository.findAll().stream()
                .filter(notification -> anotherMemberId.equals(notification.getMemberId()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/notifications")
                        .with(jwt().jwt(token -> token.subject(memberId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].referenceId").value(ownOrderId.toString()));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(jwt().jwt(token -> token.subject(memberId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/{id}/read", otherNotificationId)
                        .with(jwt().jwt(token -> token.subject(memberId.toString())))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DATA_NOT_FOUND"));

        mockMvc.perform(patch("/api/notifications/{id}/read", ownNotificationId)
                        .with(jwt().jwt(token -> token.subject(memberId.toString())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(jwt().jwt(token -> token.subject(memberId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void documentsTheNotificationApiAndCookieAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("SKALA Shop Notification Service API"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.in")
                        .value("cookie"))
                .andExpect(jsonPath("$.paths['/api/notifications'].get.summary")
                        .value("내 알림 목록"))
                .andExpect(jsonPath("$.paths['/api/notifications/{notificationId}/read']"
                        + ".patch.description").isNotEmpty());
    }

    private void send(String eventType, String key, String payload) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                "notification-integration-events",
                key,
                payload
        );
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();
    }

    private String orderPayload(UUID orderId, UUID memberId) {
        return """
                {"orderId":"%s","memberId":"%s","totalAmount":1000,
                 "occurredAt":"2026-08-11T00:00:00Z"}
                """.formatted(orderId, memberId);
    }

    private void awaitNotificationCount(long expected) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (repository.count() == expected) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(repository.count()).isEqualTo(expected);
    }
}
