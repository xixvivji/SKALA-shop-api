CREATE TABLE wallet.point_accounts (
    member_id UUID PRIMARY KEY,
    balance NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE wallet.point_transactions (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    balance_after NUMERIC(19, 2) NOT NULL CHECK (balance_after >= 0),
    reference_id UUID NOT NULL,
    command_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_point_transactions_account
        FOREIGN KEY (member_id) REFERENCES wallet.point_accounts(member_id)
);

CREATE INDEX idx_point_transactions_member_created
    ON wallet.point_transactions(member_id, created_at DESC);
