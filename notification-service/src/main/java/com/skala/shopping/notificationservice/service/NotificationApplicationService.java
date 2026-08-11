package com.skala.shopping.notificationservice.service;

import com.skala.shopping.notificationservice.domain.Notification;
import com.skala.shopping.notificationservice.domain.NotificationRepository;
import com.skala.shopping.notificationservice.web.NotificationPageResponse;
import com.skala.shopping.notificationservice.web.NotificationResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService {

    private final NotificationRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public NotificationApplicationService(NotificationRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean process(
            String fingerprint,
            String eventType,
            String topic,
            int partition,
            long offset,
            NotificationCommand command
    ) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO consumed_events
                    (fingerprint, event_type, topic, partition_number, offset_number, consumed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (fingerprint) DO NOTHING
                """,
                fingerprint,
                eventType,
                topic,
                partition,
                offset,
                Timestamp.from(clock.instant())
        );
        if (inserted == 0) {
            return false;
        }
        Instant occurredAt = command.occurredAt() == null ? clock.instant() : command.occurredAt();
        repository.save(new Notification(
                command.memberId(),
                command.type(),
                command.title(),
                command.message(),
                command.referenceType(),
                command.referenceId(),
                fingerprint,
                occurredAt
        ));
        return true;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(UUID memberId, int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        var result = repository.findByMemberId(memberId, pageable);
        return new NotificationPageResponse(
                result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID memberId) {
        return repository.countByMemberIdAndReadAtIsNull(memberId);
    }

    @Transactional
    public NotificationResponse markRead(UUID memberId, UUID notificationId) {
        Notification notification = repository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new EmptyResultDataAccessException("알림을 찾을 수 없습니다.", 1));
        notification.markRead(clock.instant());
        return NotificationResponse.from(notification);
    }
}
