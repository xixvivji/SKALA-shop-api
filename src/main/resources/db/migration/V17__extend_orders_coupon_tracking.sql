ALTER TABLE orders.orders
    ADD COLUMN IF NOT EXISTS original_amount NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS used_coupon_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tracking_carrier VARCHAR(80),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tracking_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS estimated_delivery_at TIMESTAMPTZ;

UPDATE orders.orders
SET original_amount = COALESCE(original_amount, total_amount),
    discount_amount = COALESCE(discount_amount, 0)
WHERE original_amount IS NULL OR discount_amount IS NULL;

ALTER TABLE orders.orders
    ALTER COLUMN original_amount SET NOT NULL,
    ALTER COLUMN discount_amount SET NOT NULL;

ALTER TABLE orders.orders
    ADD CONSTRAINT ck_orders_discount_non_negative
        CHECK (discount_amount >= 0),
    ADD CONSTRAINT ck_orders_original_amount_non_negative
        CHECK (original_amount >= 0),
    ADD CONSTRAINT ck_orders_amount_reconciliation
        CHECK (total_amount = original_amount - discount_amount);

ALTER TABLE orders.order_items
    ADD COLUMN paid_amount NUMERIC(19, 2),
    ADD COLUMN refunded_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE orders.order_items
SET paid_amount = unit_price * ordered_quantity
WHERE paid_amount IS NULL;

ALTER TABLE orders.order_items
    ALTER COLUMN paid_amount SET NOT NULL,
    ADD CONSTRAINT ck_order_items_paid_amount_non_negative
        CHECK (paid_amount >= 0),
    ADD CONSTRAINT ck_order_items_refunded_amount_valid
        CHECK (refunded_amount >= 0 AND refunded_amount <= paid_amount);

ALTER TABLE orders.order_cancellations
    DROP CONSTRAINT order_cancellations_refund_amount_check,
    ADD CONSTRAINT ck_order_cancellations_refund_non_negative
        CHECK (refund_amount >= 0);

CREATE SCHEMA IF NOT EXISTS coupon;

CREATE TABLE coupon.coupon_usages (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    coupon_code VARCHAR(50) NOT NULL,
    member_id UUID NOT NULL,
    order_id UUID NOT NULL UNIQUE,
    command_id UUID NOT NULL,
    discount_amount NUMERIC(19, 2) NOT NULL CHECK (discount_amount > 0),
    used_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_coupon_usage_member_code UNIQUE (member_id, coupon_code),
    CONSTRAINT uk_coupon_usage_member_command UNIQUE (member_id, command_id),
    CONSTRAINT fk_coupon_usage_order
        FOREIGN KEY (order_id) REFERENCES orders.orders(id)
);

CREATE INDEX idx_coupon_usage_member_used
    ON coupon.coupon_usages(member_id, used_at DESC);
