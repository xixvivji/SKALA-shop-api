CREATE TABLE catalog.product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES catalog.products(id),
    sku VARCHAR(100) NOT NULL UNIQUE,
    option_name VARCHAR(50),
    option_value VARCHAR(100),
    additional_price NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (additional_price >= 0),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- 기존 상품 ID를 기본 SKU ID로 재사용하여 기존 주문·재고 API와 호환합니다.
INSERT INTO catalog.product_variants
    (id, product_id, sku, option_name, option_value, additional_price, status, created_at, updated_at)
SELECT id, id, 'DEFAULT-' || id, NULL, NULL, 0, status, created_at, updated_at
FROM catalog.products;

ALTER TABLE cart.cart_items ADD COLUMN variant_id UUID;
UPDATE cart.cart_items SET variant_id = product_id;
ALTER TABLE cart.cart_items ALTER COLUMN variant_id SET NOT NULL;
ALTER TABLE cart.cart_items DROP CONSTRAINT uk_cart_items_member_product;
ALTER TABLE cart.cart_items ADD CONSTRAINT uk_cart_items_member_variant UNIQUE (member_id, variant_id);

ALTER TABLE orders.order_items ADD COLUMN variant_id UUID;
UPDATE orders.order_items SET variant_id = product_id;
ALTER TABLE orders.order_items ALTER COLUMN variant_id SET NOT NULL;
ALTER TABLE orders.order_items ADD COLUMN sku VARCHAR(100);
ALTER TABLE orders.order_items ADD COLUMN option_name VARCHAR(50);
ALTER TABLE orders.order_items ADD COLUMN option_value VARCHAR(100);
CREATE INDEX idx_order_items_variant ON orders.order_items(variant_id);
