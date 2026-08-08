CREATE SCHEMA IF NOT EXISTS returns;

CREATE TABLE returns.return_requests (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    member_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL UNIQUE,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason VARCHAR(50) NOT NULL,
    evidence_image_url VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    gross_refund_amount NUMERIC(19,2) NOT NULL CHECK (gross_refund_amount >= 0),
    shipping_fee NUMERIC(19,2) NOT NULL CHECK (shipping_fee >= 0),
    refund_amount NUMERIC(19,2) NOT NULL CHECK (refund_amount >= 0),
    point_refund_amount NUMERIC(19,2) NOT NULL CHECK (point_refund_amount >= 0),
    payment_refund_amount NUMERIC(19,2) NOT NULL CHECK (payment_refund_amount >= 0),
    balance_after NUMERIC(19,2),
    admin_note VARCHAR(500),
    requested_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    processed_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_returns_refund_split
        CHECK (point_refund_amount + payment_refund_amount = refund_amount),
    CONSTRAINT ck_returns_fee_calculation
        CHECK (gross_refund_amount - shipping_fee = refund_amount)
);

CREATE INDEX idx_returns_member_requested
    ON returns.return_requests(member_id, requested_at DESC, id DESC);
CREATE INDEX idx_returns_status_requested
    ON returns.return_requests(status, requested_at, id);
