CREATE INDEX idx_point_transactions_member_created_id
    ON wallet.point_transactions(member_id, created_at DESC, id DESC);

CREATE INDEX idx_stock_movements_product_created_id
    ON inventory.stock_movements(product_id, created_at DESC, id DESC);
