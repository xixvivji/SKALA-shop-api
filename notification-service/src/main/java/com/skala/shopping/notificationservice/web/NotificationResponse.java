package com.skala.shopping.notificationservice.web;

import com.skala.shopping.notificationservice.domain.Notification;
import com.skala.shopping.notificationservice.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public final class NotificationResponse {

    private final UUID id;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String referenceType;
    private final UUID referenceId;
    private final Instant createdAt;
    private final Instant readAt;

    public NotificationResponse(
            UUID id,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            Instant createdAt,
            Instant readAt
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    public UUID getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public boolean isRead() { return readAt != null; }
}
