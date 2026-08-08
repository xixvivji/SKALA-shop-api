package com.skala.shopping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("prod")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.health.elasticsearch.enabled=false",
                "management.health.redis.enabled=false",
                "shopping.outbox.relay-enabled=false",
                "shopping.security.jwt.secret=management-port-test-secret-at-least-32-bytes",
                "shopping.security.rate-limit.store=memory",
                "shopping.security.refresh-token.store=memory"
        }
)
class ManagementPortIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @LocalServerPort
    int applicationPort;

    @LocalManagementPort
    int managementPort;

    @Test
    void exposesMetricsOnlyThroughTheDedicatedManagementPort() throws Exception {
        assertNotEquals(applicationPort, managementPort);

        HttpResponse<String> invalidRequest = get(applicationPort, "/api/products?page=-1&size=20");
        assertEquals(400, invalidRequest.statusCode());

        HttpResponse<String> managementMetrics = get(managementPort, "/actuator/prometheus");
        assertEquals(200, managementMetrics.statusCode());
        assertTrue(managementMetrics.body().contains("jvm_memory_used_bytes"));
        assertTrue(managementMetrics.body().contains("shopping_business_errors_total"));
        assertTrue(managementMetrics.body().contains("code=\"INVALID_PARAMETER\""));
        assertTrue(managementMetrics.body().contains("application=\"skala-shop-api\""));

        HttpResponse<String> applicationMetrics = get(applicationPort, "/actuator/prometheus");
        assertNotEquals(200, applicationMetrics.statusCode());
        assertFalse(applicationMetrics.body().contains("jvm_memory_used_bytes"));
    }

    @Test
    void keepsHealthAvailableAndDoesNotExposeOtherManagementEndpoints() throws Exception {
        HttpResponse<String> health = get(managementPort, "/actuator/health");
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("\"status\":\"UP\""));

        HttpResponse<String> info = get(managementPort, "/actuator/info");
        assertNotEquals(200, info.statusCode());
        assertFalse(info.body().contains("build"));
    }

    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
