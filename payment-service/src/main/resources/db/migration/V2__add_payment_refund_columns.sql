ALTER TABLE payments ADD COLUMN refund_idempotency_key VARCHAR(150) NULL;
ALTER TABLE payments ADD COLUMN refund_reason VARCHAR(500) NULL;
ALTER TABLE payments ADD COLUMN refunded_at DATETIME(6) NULL;
ALTER TABLE payments
    ADD CONSTRAINT uk_payments_refund_idempotency_key UNIQUE (refund_idempotency_key);
