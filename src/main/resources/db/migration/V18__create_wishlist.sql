CREATE SCHEMA IF NOT EXISTS wishlist;

CREATE TABLE wishlist.wishlist_items (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_wishlist_member_product UNIQUE (member_id, product_id)
);

CREATE INDEX idx_wishlist_member_created
    ON wishlist.wishlist_items(member_id, created_at DESC, id DESC);
