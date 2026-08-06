ALTER TABLE orders.orders
    ADD COLUMN fulfillment_status VARCHAR(30) NOT NULL DEFAULT 'PAID';

ALTER TABLE orders.orders ALTER COLUMN request_fingerprint TYPE VARCHAR(2048);
ALTER TABLE orders.order_items ADD COLUMN line_number INTEGER;

WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY order_id ORDER BY id) - 1 AS line_number
    FROM orders.order_items
)
UPDATE orders.order_items AS item SET line_number = numbered.line_number
FROM numbered WHERE numbered.id = item.id;

ALTER TABLE orders.order_items
    ALTER COLUMN line_number SET NOT NULL,
    ADD CONSTRAINT ck_order_items_line_number CHECK (line_number >= 0),
    ADD CONSTRAINT uk_order_items_order_line UNIQUE (order_id, line_number);

CREATE TABLE orders.order_shipping_addresses (
    order_id UUID PRIMARY KEY,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(300) NOT NULL,
    address_line2 VARCHAR(300),
    CONSTRAINT fk_order_shipping_addresses_order
        FOREIGN KEY (order_id) REFERENCES orders.orders(id) ON DELETE CASCADE
);

CREATE TABLE orders.order_status_histories (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_order_status_histories_order
        FOREIGN KEY (order_id) REFERENCES orders.orders(id) ON DELETE CASCADE
);

INSERT INTO orders.order_status_histories (id, order_id, from_status, to_status, changed_by, changed_at)
SELECT gen_random_uuid(), id, NULL, 'PAID', NULL, ordered_at FROM orders.orders;

CREATE INDEX idx_order_status_history_order_changed
    ON orders.order_status_histories(order_id, changed_at ASC, id ASC);
CREATE INDEX idx_orders_fulfillment_ordered
    ON orders.orders(fulfillment_status, ordered_at DESC, id DESC);
