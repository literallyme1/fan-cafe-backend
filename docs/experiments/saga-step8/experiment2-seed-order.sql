DROP PROCEDURE IF EXISTS seed_saga_step8_recovery;

SET @seed_count = COALESCE(@seed_count, 4000);

DELIMITER $$
CREATE PROCEDURE seed_saga_step8_recovery()
BEGIN
    DECLARE sequence_no INT DEFAULT 0;
    DECLARE current_order_id BIGINT;
    DECLARE experiment_user_id BIGINT DEFAULT 8100001;
    DECLARE experiment_product_id BIGINT DEFAULT 8100001;

    INSERT INTO users (
        id, email, password, nickname, role, avatar_url, introduction,
        password_updated_at_epoch_sec, password_set, follower_count, following_count,
        created_at, updated_at, deleted_at
    ) VALUES (
        experiment_user_id, 'saga-step8-recovery@fan-cafe.test',
        'not-used-by-recovery-experiment', 'sagaStep8R', 'USER', NULL, '',
        UNIX_TIMESTAMP(), 1, 0, 0, NOW(6), NOW(6), NULL
    )
    ON DUPLICATE KEY UPDATE deleted_at = NULL, updated_at = NOW(6);

    INSERT INTO merchandises (
        id, name, description, price, sale_price, stock, status, image_url, category,
        created_at, updated_at, deleted_at
    ) VALUES (
        experiment_product_id, '[SAGA-STEP8-EXP2] Product',
        '4,000 due PAYMENT_UNKNOWN Saga recovery experiment',
        10000, 9000, 10000, 'SALE', NULL, 'CLOTHES', NOW(6), NOW(6), NULL
    )
    ON DUPLICATE KEY UPDATE
        stock = VALUES(stock), status = 'SALE', deleted_at = NULL, updated_at = NOW(6);

    WHILE sequence_no < @seed_count DO
        SET current_order_id = 8100001 + sequence_no;

        INSERT INTO orders (
            id, user_id, total_price, status, created_at, updated_at, deleted_at
        ) VALUES (
            current_order_id, experiment_user_id, 9000.00, 'PAYMENT_PENDING',
            NOW(6), NOW(6), NULL
        );

        INSERT INTO order_items (
            order_id, product_id, product_name, price, quantity,
            created_at, updated_at, deleted_at
        ) VALUES (
            current_order_id, experiment_product_id, '[SAGA-STEP8-EXP2] Product',
            9000.00, 1, NOW(6), NOW(6), NULL
        );

        INSERT INTO saga_instance (
            saga_id, order_id, status, current_step, retry_count,
            next_retry_at, last_error, payment_unknown_at, resolved_at,
            created_at, updated_at
        ) VALUES (
            CONCAT('81000000-0000-0000-0000-', LPAD(current_order_id, 12, '0')),
            current_order_id, 'PAYMENT_UNKNOWN', 'PAYMENT_STATUS_CHECK', 0,
            DATE_SUB(NOW(6), INTERVAL 1 SECOND), 'step8 recovery experiment seed',
            DATE_SUB(NOW(6), INTERVAL 1 SECOND), NULL, NOW(6), NOW(6)
        );

        SET sequence_no = sequence_no + 1;
    END WHILE;
END$$
DELIMITER ;

CALL seed_saga_step8_recovery();
DROP PROCEDURE seed_saga_step8_recovery;

SELECT status, COUNT(*) AS seeded_saga_count
FROM saga_instance
WHERE order_id BETWEEN 8100001 AND 8100000 + @seed_count
GROUP BY status;
