CREATE TABLE catalog.products (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    price NUMERIC(19, 2) NOT NULL CHECK (price > 0),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_products_status ON catalog.products(status);
