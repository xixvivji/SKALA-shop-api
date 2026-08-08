package com.skala.shopping.outbox.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusAndNextAttemptAtLessThanEqualOrderByOccurredAtAscIdAsc(
            OutboxStatus status, Instant now, Pageable pageable);
}
