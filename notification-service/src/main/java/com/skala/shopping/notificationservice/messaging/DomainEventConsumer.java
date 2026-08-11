package com.skala.shopping.notificationservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.notificationservice.domain.NotificationType;
import com.skala.shopping.notificationservice.service.NotificationApplicationService;
import com.skala.shopping.notificationservice.service.NotificationCommand;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DomainEventConsumer.class);
    private static final String ORDER_PLACED = "com.skala.shopping.order.OrderPlaced";
    private static final String STOCK_ALERT_TRIGGERED =
            "com.skala.shopping.stockalert.StockAlertTriggered";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(ORDER_PLACED, STOCK_ALERT_TRIGGERED);

    private final ObjectMapper objectMapper;
    private final NotificationApplicationService service;
    private final String eventTypeHeader;

    public DomainEventConsumer(
            ObjectMapper objectMapper,
            NotificationApplicationService service,
            @Value("${shopping.notification.event-type-header:eventType}") String eventTypeHeader
    ) {
        this.objectMapper = objectMapper;
        this.service = service;
        this.eventTypeHeader = eventTypeHeader;
    }

    @KafkaListener(topics = "${shopping.notification.kafka-topic}")
    public void consume(ConsumerRecord<String, String> record) {
        String eventType = eventType(record);
        if (!SUPPORTED_EVENTS.contains(eventType)) {
            return;
        }
        try {
            String fingerprint = fingerprint(eventType, record.key(), record.value());
            NotificationCommand command = command(eventType, record.value());
            boolean created = service.process(
                    fingerprint,
                    eventType,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    command
            );
            log.info(
                    "notification_event_processed eventType={} fingerprint={} created={} partition={} offset={}",
                    eventType,
                    fingerprint,
                    created,
                    record.partition(),
                    record.offset()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "notification_event_processing_failed eventType={} partition={} offset={}",
                    eventType,
                    record.partition(),
                    record.offset(),
                    exception
            );
            throw exception;
        }
    }

    private NotificationCommand command(String eventType, String payload) {
        try {
            if (ORDER_PLACED.equals(eventType)) {
                OrderPlacedMessage event = objectMapper.readValue(payload, OrderPlacedMessage.class);
                require(event.getOrderId(), "orderId");
                require(event.getMemberId(), "memberId");
                String amount = event.getTotalAmount() == null
                        ? "0"
                        : event.getTotalAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
                return new NotificationCommand(
                        event.getMemberId(),
                        NotificationType.ORDER_PLACED,
                        "주문이 접수되었습니다",
                        "주문 금액 " + amount + "원의 주문이 접수되었습니다.",
                        "ORDER",
                        event.getOrderId(),
                        event.getOccurredAt()
                );
            }
            StockAlertTriggeredMessage event = objectMapper.readValue(
                    payload,
                    StockAlertTriggeredMessage.class
            );
            require(event.getSubscriptionId(), "subscriptionId");
            require(event.getMemberId(), "memberId");
            require(event.getProductId(), "productId");
            return new NotificationCommand(
                    event.getMemberId(),
                    NotificationType.STOCK_REPLENISHED,
                    "상품이 다시 입고되었습니다",
                    "신청한 상품의 재고 " + event.getAvailableQuantity() + "개가 준비되었습니다.",
                    "PRODUCT",
                    event.getProductId(),
                    event.getOccurredAt()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("알림 이벤트 JSON을 해석하지 못했습니다.", exception);
        }
    }

    private String eventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(eventTypeHeader);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }

    private String fingerprint(String eventType, String key, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = eventType + '\0' + (key == null ? "" : key) + '\0' + payload;
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private void require(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("알림 이벤트에 " + field + "가 없습니다.");
        }
    }
}
