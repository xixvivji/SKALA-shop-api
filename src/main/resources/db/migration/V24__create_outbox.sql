CREATE SCHEMA IF NOT EXISTS outbox;
CREATE TABLE outbox.outbox_events (
    id UUID PRIMARY KEY, aggregate_type VARCHAR(100) NOT NULL, aggregate_id UUID NOT NULL,
    event_type VARCHAR(200) NOT NULL, payload TEXT NOT NULL, status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0, next_attempt_at TIMESTAMPTZ NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL, published_at TIMESTAMPTZ, last_error VARCHAR(1000)
);
CREATE INDEX idx_outbox_pending ON outbox.outbox_events(status, next_attempt_at, occurred_at, id);
