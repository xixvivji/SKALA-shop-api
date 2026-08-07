DROP INDEX IF EXISTS orders.idx_orders_member_ordered;

CREATE INDEX idx_orders_member_ordered_id
    ON orders.orders(member_id, ordered_at DESC, id DESC);
