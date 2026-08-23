SET @add_payment_unknown_at = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'saga_instance'
          AND column_name = 'payment_unknown_at'
    ),
    'SELECT 1',
    'ALTER TABLE saga_instance ADD COLUMN payment_unknown_at DATETIME(6) NULL AFTER last_error'
);
PREPARE step8_payment_unknown_at FROM @add_payment_unknown_at;
EXECUTE step8_payment_unknown_at;
DEALLOCATE PREPARE step8_payment_unknown_at;

SET @add_resolved_at = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'saga_instance'
          AND column_name = 'resolved_at'
    ),
    'SELECT 1',
    'ALTER TABLE saga_instance ADD COLUMN resolved_at DATETIME(6) NULL AFTER payment_unknown_at'
);
PREPARE step8_resolved_at FROM @add_resolved_at;
EXECUTE step8_resolved_at;
DEALLOCATE PREPARE step8_resolved_at;
