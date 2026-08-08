ALTER TABLE payment.payments
    ADD COLUMN approve_fingerprint VARCHAR(80),
    ADD COLUMN approve_result_status VARCHAR(30),
    ADD COLUMN approve_result_failure_code VARCHAR(50),
    ADD COLUMN approve_result_failure_message VARCHAR(200),
    ADD COLUMN approve_result_transaction_id VARCHAR(100),
    ADD COLUMN approve_result_approved_amount NUMERIC(19, 2),
    ADD COLUMN approve_result_refunded_amount NUMERIC(19, 2),
    ADD COLUMN approve_result_approved_at TIMESTAMPTZ;
