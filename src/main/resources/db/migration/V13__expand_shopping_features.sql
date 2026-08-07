CREATE SCHEMA IF NOT EXISTS cart;

CREATE TABLE member.member_addresses (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    address_name VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(300) NOT NULL,
    address_line2 VARCHAR(300),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_member_addresses_member
        FOREIGN KEY (member_id) REFERENCES member.members(id),
    CONSTRAINT uk_member_addresses_member_name
        UNIQUE (member_id, address_name)
);

CREATE INDEX idx_member_addresses_member_default
    ON member.member_addresses(member_id, is_default DESC, created_at ASC, id ASC);

CREATE UNIQUE INDEX uk_member_addresses_one_default
    ON member.member_addresses(member_id)
    WHERE is_default = TRUE;

CREATE TABLE cart.carts (
    member_id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cart.cart_items (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0 AND quantity <= 1000000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (member_id) REFERENCES cart.carts(member_id) ON DELETE CASCADE,
    CONSTRAINT uk_cart_items_member_product
        UNIQUE (member_id, product_id)
);

CREATE INDEX idx_cart_items_member_created
    ON cart.cart_items(member_id, created_at ASC, id ASC);
