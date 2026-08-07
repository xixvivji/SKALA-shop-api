CREATE SCHEMA IF NOT EXISTS stockalert;

CREATE TABLE stockalert.stock_alert_subscriptions (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_stockalert_member_product UNIQUE (member_id, product_id)
);

CREATE INDEX idx_stockalert_member_created
    ON stockalert.stock_alert_subscriptions(member_id, created_at DESC, id DESC);
