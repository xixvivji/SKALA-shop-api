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
    ADD CONSTRAINT IF NOT EXISTS ck_orders_discount_non_negative
        CHECK (discount_amount >= 0);
