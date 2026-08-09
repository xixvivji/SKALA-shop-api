package com.skala.shopping.outbox.internal;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Outbox의 aggregate ID를 Kafka key로 사용해 같은 aggregate 이벤트 순서를 보존합니다. */
@Component
@ConditionalOnProperty(name="shopping.outbox.publisher",havingValue="kafka")
class KafkaOutboxMessagePublisher implements OutboxMessagePublisher {
    private final KafkaTemplate<String,String> kafka; private final String topic; private final Duration timeout;
    KafkaOutboxMessagePublisher(KafkaTemplate<String,String> kafka,
            @Value("${shopping.outbox.kafka-topic:skala-shop.domain-events}") String topic,
            @Value("${shopping.outbox.kafka-timeout:5s}") Duration timeout){this.kafka=kafka;this.topic=topic;this.timeout=timeout;}
    public void publish(String key,String eventType,String payload){
        ProducerRecord<String,String> record=new ProducerRecord<>(topic,key,payload);
        record.headers().add("eventType",eventType.getBytes(StandardCharsets.UTF_8));
        try{kafka.send(record).get(timeout.toMillis(), TimeUnit.MILLISECONDS);}
        catch(InterruptedException exception){Thread.currentThread().interrupt();throw new IllegalStateException("Kafka 이벤트 발행이 중단됐습니다: "+eventType,exception);}
        catch(Exception exception){throw new IllegalStateException("Kafka 이벤트 발행에 실패했습니다: "+eventType,exception);}
    }
}
