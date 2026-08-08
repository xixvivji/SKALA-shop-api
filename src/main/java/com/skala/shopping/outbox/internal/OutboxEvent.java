package com.skala.shopping.outbox.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="outbox_events", schema="outbox")
class OutboxEvent {
    @Id private UUID id;
    @Column(name="aggregate_type",nullable=false,length=100) private String aggregateType;
    @Column(name="aggregate_id",nullable=false) private UUID aggregateId;
    @Column(name="event_type",nullable=false,length=200) private String eventType;
    @Column(nullable=false,columnDefinition="text") private String payload;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OutboxStatus status;
    @Column(name="retry_count",nullable=false) private int retryCount;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @Column(name="published_at") private Instant publishedAt;
    @Column(name="last_error",length=1000) private String lastError;
    protected OutboxEvent() { }
    OutboxEvent(String aggregateType, UUID aggregateId, Object event, String payload, Instant now) {
        id=UUID.randomUUID(); this.aggregateType=aggregateType; this.aggregateId=aggregateId;
        eventType=event.getClass().getName(); this.payload=payload; status=OutboxStatus.PENDING;
        retryCount=0; nextAttemptAt=now; occurredAt=now;
    }
    UUID id(){return id;} String eventType(){return eventType;} String payload(){return payload;}
    UUID aggregateId(){return aggregateId;}
    void published(Instant now){status=OutboxStatus.PUBLISHED; publishedAt=now; lastError=null;}
    void failed(String message, Instant now){retryCount++; lastError=message == null ? "unknown" : message.substring(0,Math.min(1000,message.length()));
        if(retryCount>=10){status=OutboxStatus.DEAD;} else {nextAttemptAt=now.plusSeconds(Math.min(300,1L<<Math.min(retryCount,8)));}}
}
