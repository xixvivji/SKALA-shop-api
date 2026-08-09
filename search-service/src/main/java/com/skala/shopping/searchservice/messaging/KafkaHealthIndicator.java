package com.skala.shopping.searchservice.messaging;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Search Service가 이벤트를 받을 수 없는 상태를 정상 health로 승격하지 않습니다. */
@Component
public class KafkaHealthIndicator implements HealthIndicator, AutoCloseable {

    private final Admin admin;
    private final Duration timeout;

    @Autowired
    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${shopping.search.kafka-health-timeout:2s}") Duration timeout
    ) {
        this(
                AdminClient.create(Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) timeout.toMillis(),
                        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) timeout.toMillis()
                )),
                timeout
        );
    }

    KafkaHealthIndicator(Admin admin, Duration timeout) {
        this.admin = admin;
        this.timeout = timeout;
    }

    @Override
    public Health health() {
        try {
            String clusterId = admin.describeCluster()
                    .clusterId()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return Health.up().withDetail("clusterId", clusterId).build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }

    @Override
    public void close() {
        admin.close(timeout);
    }
}
