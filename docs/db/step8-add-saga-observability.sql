ALTER TABLE saga_instance
    ADD COLUMN payment_unknown_at DATETIME(6) NULL AFTER last_error,
    ADD COLUMN resolved_at DATETIME(6) NULL AFTER payment_unknown_at;
