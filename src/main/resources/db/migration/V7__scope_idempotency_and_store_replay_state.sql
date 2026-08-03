DO $$
BEGIN
    IF EXISTS (
        SELECT shop_order.id
        FROM orders.orders AS shop_order
        LEFT JOIN orders.order_items AS order_item
            ON order_item.order_id = shop_order.id
        GROUP BY shop_order.id
        HAVING COUNT(order_item.id) <> 1
    ) THEN
        RAISE EXCEPTION 'Each existing order must have exactly one item before V7';
    END IF;
END
$$;

ALTER TABLE orders.orders
    ADD COLUMN request_fingerprint VARCHAR(128),
    ADD COLUMN balance_after NUMERIC(19, 2);

UPDATE orders.orders AS shop_order
SET request_fingerprint = 'ORDER|'
        || shop_order.member_id::TEXT || '|'
        || order_item.product_id::TEXT || '|'
        || order_item.ordered_quantity::TEXT
FROM orders.order_items AS order_item
WHERE order_item.order_id = shop_order.id;

UPDATE orders.orders AS shop_order
SET balance_after = point_transaction.balance_after
FROM wallet.point_transactions AS point_transaction
WHERE point_transaction.member_id = shop_order.member_id
  AND point_transaction.command_id = shop_order.request_id
  AND point_transaction.transaction_type = 'DEBIT';

ALTER TABLE orders.order_cancellations
    ADD COLUMN request_fingerprint VARCHAR(128),
    ADD COLUMN balance_after NUMERIC(19, 2);

UPDATE orders.order_cancellations AS cancellation
SET request_fingerprint = 'CANCEL|'
        || cancellation.member_id::TEXT || '|'
        || cancellation.product_id::TEXT || '|'
        || cancellation.quantity::TEXT;

UPDATE orders.order_cancellations AS cancellation
SET balance_after = point_transaction.balance_after
FROM wallet.point_transactions AS point_transaction
WHERE point_transaction.member_id = cancellation.member_id
  AND point_transaction.command_id = cancellation.command_id
  AND point_transaction.transaction_type = 'REFUND';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM orders.orders
        WHERE request_fingerprint IS NULL OR balance_after IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot backfill order idempotency replay state';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM orders.order_cancellations
        WHERE request_fingerprint IS NULL OR balance_after IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot backfill cancellation idempotency replay state';
    END IF;
END
$$;

ALTER TABLE orders.orders
    ALTER COLUMN request_fingerprint SET NOT NULL,
    ALTER COLUMN balance_after SET NOT NULL;

ALTER TABLE orders.order_cancellations
    ALTER COLUMN request_fingerprint SET NOT NULL,
    ALTER COLUMN balance_after SET NOT NULL;

ALTER TABLE orders.orders
    DROP CONSTRAINT orders_request_id_key,
    ADD CONSTRAINT uk_orders_member_request UNIQUE (member_id, request_id);

ALTER TABLE orders.order_cancellations
    DROP CONSTRAINT order_cancellations_command_id_key,
    ADD CONSTRAINT uk_order_cancellations_member_command UNIQUE (member_id, command_id);

ALTER TABLE wallet.point_transactions
    DROP CONSTRAINT point_transactions_command_id_key,
    ADD CONSTRAINT uk_point_transactions_member_command_type
        UNIQUE (member_id, command_id, transaction_type);
