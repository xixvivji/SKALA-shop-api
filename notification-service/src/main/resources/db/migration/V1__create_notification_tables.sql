CREATE TABLE consumed_events (
    fingerprint VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(200) NOT NULL,
    topic VARCHAR(200) NOT NULL,
    partition_number INTEGER NOT NULL,
    offset_number BIGINT NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_type VARCHAR(40) NOT NULL,
    reference_id UUID NOT NULL,
    event_fingerprint VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT fk_notifications_consumed_event
        FOREIGN KEY (event_fingerprint) REFERENCES consumed_events(fingerprint)
);

CREATE INDEX idx_notifications_member_created
    ON notifications (member_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_member_unread
    ON notifications (member_id, created_at DESC)
    WHERE read_at IS NULL;
