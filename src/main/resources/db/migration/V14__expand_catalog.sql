CREATE TABLE catalog.categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_categories_active_name_ci
    ON catalog.categories (LOWER(name))
    WHERE status = 'ACTIVE';

ALTER TABLE catalog.products
    ADD COLUMN category_id UUID,
    ADD COLUMN description VARCHAR(2000),
    ADD COLUMN image_url VARCHAR(1000),
    ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES catalog.categories(id);

CREATE INDEX idx_products_category_status_created
    ON catalog.products(category_id, status, created_at DESC, id DESC);
