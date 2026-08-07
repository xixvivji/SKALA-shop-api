CREATE SCHEMA IF NOT EXISTS reviews;

CREATE TABLE reviews.product_reviews (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    product_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_reviews_member_product UNIQUE (member_id, product_id)
);

CREATE INDEX idx_reviews_product_created
    ON reviews.product_reviews(product_id, created_at DESC, id DESC);

CREATE INDEX idx_reviews_member_created
    ON reviews.product_reviews(member_id, created_at DESC, id DESC);
