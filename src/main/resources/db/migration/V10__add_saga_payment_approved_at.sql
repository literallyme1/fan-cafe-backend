ALTER TABLE saga_instance
    ADD COLUMN payment_approved_at DATETIME(6) NULL AFTER payment_unknown_at;

UPDATE saga_instance
SET next_retry_at = CURRENT_TIMESTAMP(6)
WHERE status = 'PAYMENT_COMPLETED'
  AND next_retry_at IS NULL;
