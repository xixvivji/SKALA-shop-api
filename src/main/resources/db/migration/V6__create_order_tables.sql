CREATE TABLE orders.orders (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    member_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL CHECK (total_amount >= 0),
    canceled_amount NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (canceled_amount >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    ordered_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE orders.order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price > 0),
    ordered_quantity INTEGER NOT NULL CHECK (ordered_quantity > 0),
    canceled_quantity INTEGER NOT NULL DEFAULT 0 CHECK (canceled_quantity >= 0),
    CONSTRAINT ck_order_item_cancel_quantity
        CHECK (canceled_quantity <= ordered_quantity),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders.orders(id)
);

CREATE TABLE orders.order_cancellations (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    member_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    refund_amount NUMERIC(19, 2) NOT NULL CHECK (refund_amount > 0),
    canceled_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_orders_member_ordered
    ON orders.orders(member_id, ordered_at DESC);
CREATE INDEX idx_order_items_product
    ON orders.order_items(product_id);
