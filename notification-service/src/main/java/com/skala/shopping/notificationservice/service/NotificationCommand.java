package com.skala.shopping.notificationservice.service;

import com.skala.shopping.notificationservice.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

public final class NotificationCommand {

    private final UUID memberId;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String referenceType;
    private final UUID referenceId;
    private final Instant occurredAt;

    public NotificationCommand(
            UUID memberId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            Instant occurredAt
    ) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    public UUID memberId() { return memberId; }
    public NotificationType type() { return type; }
    public String title() { return title; }
    public String message() { return message; }
    public String referenceType() { return referenceType; }
    public UUID referenceId() { return referenceId; }
    public Instant occurredAt() { return occurredAt; }
}
