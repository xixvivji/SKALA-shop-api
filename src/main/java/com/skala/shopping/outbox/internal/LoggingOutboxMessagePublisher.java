package com.skala.shopping.outbox.internal;

import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name="shopping.outbox.publisher",havingValue="logging",matchIfMissing=true)
class LoggingOutboxMessagePublisher implements OutboxMessagePublisher {
    private static final Logger log=LoggerFactory.getLogger(LoggingOutboxMessagePublisher.class);
    public void publish(String key,String eventType,String payload){log.info("outbox_event_published key={} eventType={} payload={}",key,eventType,payload);}
}
