package com.skala.shopping.searchservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.searchservice.service.ProductSearchService;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductSearchEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchEventConsumer.class);
    private static final String EVENT_TYPE_HEADER = "eventType";

    private final ObjectMapper objectMapper;
    private final ProductSearchService service;
    private final String productEventType;

    public ProductSearchEventConsumer(
            ObjectMapper objectMapper,
            ProductSearchService service,
            @Value("${shopping.search.product-event-type}") String productEventType
    ) {
        this.objectMapper = objectMapper;
        this.service = service;
        this.productEventType = productEventType;
    }

    @KafkaListener(topics = "${shopping.search.kafka-topic}")
    public void consume(ConsumerRecord<String, String> record) {
        String eventType = eventType(record);
        if (!productEventType.equals(eventType)) {
            return;
        }
        try {
            ProductSearchChangedMessage event = objectMapper.readValue(
                    record.value(),
                    ProductSearchChangedMessage.class
            );
            service.apply(event);
            log.info("product_search_event_applied productId={} partition={} offset={}",
                    event.getId(), record.partition(), record.offset());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("상품 검색 이벤트 JSON을 해석하지 못했습니다.", exception);
        }
    }

    private String eventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
