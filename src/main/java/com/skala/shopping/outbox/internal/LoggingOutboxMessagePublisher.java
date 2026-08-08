package com.skala.shopping.outbox.internal;

import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.stereotype.Component;

@Component
class LoggingOutboxMessagePublisher implements OutboxMessagePublisher {
    private static final Logger log=LoggerFactory.getLogger(LoggingOutboxMessagePublisher.class);
    public void publish(String eventType,String payload){log.info("outbox_event_published eventType={} payload={}",eventType,payload);}
}
