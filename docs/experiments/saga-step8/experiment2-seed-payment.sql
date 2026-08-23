DROP PROCEDURE IF EXISTS seed_saga_step8_payments;

DELIMITER $$
CREATE PROCEDURE seed_saga_step8_payments()
BEGIN
    DECLARE sequence_no INT DEFAULT 0;
    DECLARE current_order_id BIGINT;

    WHILE sequence_no < 4000 DO
        SET current_order_id = 8100001 + sequence_no;
        INSERT INTO payments (
            order_id, status, expected_amount, approved_amount, payment_key,
            failure_reason, refund_idempotency_key, refund_reason, refunded_at,
            version, created_at, updated_at
        ) VALUES (
            current_order_id, 'APPROVED', 9000.00, 9000.00,
            CONCAT('STEP8-RECOVERY-', current_order_id),
            NULL, NULL, NULL, NULL, 0, NOW(6), NOW(6)
        );
        SET sequence_no = sequence_no + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_saga_step8_payments();
DROP PROCEDURE seed_saga_step8_payments;

SELECT status, COUNT(*) AS seeded_payment_count
FROM payments
WHERE order_id BETWEEN 8100001 AND 8104000
GROUP BY status;
