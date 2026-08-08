ALTER TABLE returns.return_requests
    DROP CONSTRAINT IF EXISTS return_requests_order_item_id_key;

CREATE INDEX IF NOT EXISTS idx_returns_order_item_status
    ON returns.return_requests(order_item_id, status);

CREATE TABLE returns.return_status_commands (
    command_id UUID PRIMARY KEY,
    return_id UUID NOT NULL REFERENCES returns.return_requests(id),
    admin_id UUID NOT NULL,
    requested_status VARCHAR(30) NOT NULL,
    requested_admin_note VARCHAR(500),
    result_status VARCHAR(30) NOT NULL,
    result_balance_after NUMERIC(19,2),
    result_admin_note VARCHAR(500),
    result_updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_return_status_commands_return
    ON returns.return_status_commands(return_id);
