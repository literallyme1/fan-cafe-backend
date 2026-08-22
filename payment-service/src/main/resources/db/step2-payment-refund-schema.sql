ALTER TABLE payments
    ADD COLUMN refund_idempotency_key VARCHAR(150) NULL,
    ADD COLUMN refund_reason VARCHAR(500) NULL,
    ADD COLUMN refunded_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_payments_refund_idempotency_key UNIQUE (refund_idempotency_key);

