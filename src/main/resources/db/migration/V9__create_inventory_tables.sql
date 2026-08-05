CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.stocks (
    product_id UUID PRIMARY KEY,
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory.stock_movements (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    product_id UUID NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    available_after INTEGER NOT NULL CHECK (available_after >= 0),
    request_fingerprint VARCHAR(512) NOT NULL,
    reason VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_stock_movements_stock
        FOREIGN KEY (product_id) REFERENCES inventory.stocks(product_id),
    CONSTRAINT uk_stock_movements_operation_product
        UNIQUE (operation_id, product_id)
);

CREATE INDEX idx_stock_movements_product_created
    ON inventory.stock_movements(product_id, created_at DESC);
