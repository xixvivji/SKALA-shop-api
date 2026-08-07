CREATE SCHEMA IF NOT EXISTS payment;

ALTER TABLE orders.orders
    ADD COLUMN point_used_amount NUMERIC(19, 2),
    ADD COLUMN payment_amount NUMERIC(19, 2);

UPDATE orders.orders
SET point_used_amount = total_amount,
    payment_amount = 0
WHERE point_used_amount IS NULL OR payment_amount IS NULL;

ALTER TABLE orders.orders
    ALTER COLUMN point_used_amount SET NOT NULL,
    ALTER COLUMN payment_amount SET NOT NULL,
    ADD CONSTRAINT ck_orders_payment_split_non_negative
        CHECK (point_used_amount >= 0 AND payment_amount >= 0),
    ADD CONSTRAINT ck_orders_payment_split_total
        CHECK (point_used_amount + payment_amount = total_amount);

CREATE TABLE payment.payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    member_id UUID NOT NULL,
    prepare_command_id UUID NOT NULL,
    prepare_fingerprint VARCHAR(300) NOT NULL,
    approve_command_id UUID,
    provider VARCHAR(30) NOT NULL,
    provider_transaction_id VARCHAR(100),
    method VARCHAR(30) NOT NULL,
    masked_number VARCHAR(30),
    requested_amount NUMERIC(19, 2) NOT NULL CHECK (requested_amount > 0),
    approved_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (approved_amount >= 0),
    refunded_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    status VARCHAR(30) NOT NULL,
    failure_code VARCHAR(50),
    failure_message VARCHAR(200),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payments_order UNIQUE (order_id),
    CONSTRAINT uk_payments_member_prepare UNIQUE (member_id, prepare_command_id),
    CONSTRAINT ck_payments_amounts CHECK (
        approved_amount <= requested_amount
        AND refunded_amount <= approved_amount
    )
);

CREATE INDEX idx_payments_member_created
    ON payment.payments(member_id, created_at DESC, id DESC);
CREATE INDEX idx_payments_status_updated
    ON payment.payments(status, updated_at, id);

CREATE TABLE payment.payment_refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    command_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payment_refunds_payment
        FOREIGN KEY (payment_id) REFERENCES payment.payments(id)
);

CREATE INDEX idx_payment_refunds_payment_created
    ON payment.payment_refunds(payment_id, created_at, id);

CREATE TABLE payment.payment_webhook_events (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payment_webhooks_payment
        FOREIGN KEY (payment_id) REFERENCES payment.payments(id)
);
