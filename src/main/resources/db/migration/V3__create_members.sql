CREATE TABLE member.members (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
