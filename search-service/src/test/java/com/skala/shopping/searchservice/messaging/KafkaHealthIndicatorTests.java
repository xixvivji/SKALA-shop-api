package com.skala.shopping.searchservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class KafkaHealthIndicatorTests {

    @Test
    void reportsUpWhenKafkaClusterResponds() throws Exception {
        Admin admin = mock(Admin.class);
        DescribeClusterResult result = mock(DescribeClusterResult.class);
        @SuppressWarnings("unchecked")
        KafkaFuture<String> clusterId = mock(KafkaFuture.class);
        when(admin.describeCluster()).thenReturn(result);
        when(result.clusterId()).thenReturn(clusterId);
        when(clusterId.get(1000, TimeUnit.MILLISECONDS)).thenReturn("cluster-1");

        var indicator = new KafkaHealthIndicator(admin, Duration.ofSeconds(1));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenKafkaDoesNotRespond() throws Exception {
        Admin admin = mock(Admin.class);
        DescribeClusterResult result = mock(DescribeClusterResult.class);
        @SuppressWarnings("unchecked")
        KafkaFuture<String> clusterId = mock(KafkaFuture.class);
        when(admin.describeCluster()).thenReturn(result);
        when(result.clusterId()).thenReturn(clusterId);
        when(clusterId.get(1000, TimeUnit.MILLISECONDS)).thenThrow(new RuntimeException("offline"));

        var indicator = new KafkaHealthIndicator(admin, Duration.ofSeconds(1));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
