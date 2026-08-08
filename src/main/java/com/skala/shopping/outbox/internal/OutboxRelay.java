package com.skala.shopping.outbox.internal;

import java.time.Clock; import org.springframework.data.domain.PageRequest; import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name="shopping.outbox.relay-enabled",havingValue="true")
class OutboxRelay {
    private final OutboxEventRepository repository; private final OutboxMessagePublisher publisher; private final Clock clock=Clock.systemUTC();
    OutboxRelay(OutboxEventRepository repository,OutboxMessagePublisher publisher){this.repository=repository;this.publisher=publisher;}
    @Scheduled(fixedDelayString="${shopping.outbox.poll-interval:1s}") @Transactional
    public void relay(){for(OutboxEvent event:repository.findByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAscIdAsc(
            OutboxStatus.PENDING,clock.instant(),PageRequest.of(0,100))){try{publisher.publish(event.eventType(),event.payload());event.published(clock.instant());}
        catch(RuntimeException exception){event.failed(exception.getMessage(),clock.instant());}}}
}
