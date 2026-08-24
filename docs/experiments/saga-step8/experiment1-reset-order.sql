SET @product_id = 8000001;
SET @experiment_email = 'saga-step8@fan-cafe.test';
SET @order_id_start = 8200001;
SET @expected_order_count = 20000;

CREATE TEMPORARY TABLE step8_experiment1_order_ids (
    order_id BIGINT PRIMARY KEY
);

INSERT INTO step8_experiment1_order_ids (order_id)
SELECT orders.id
FROM orders
JOIN users ON users.id = orders.user_id
WHERE users.email = @experiment_email;

SET @reset_order_count = (SELECT COUNT(*) FROM step8_experiment1_order_ids);

DELETE FROM saga_instance
WHERE order_id IN (SELECT order_id FROM step8_experiment1_order_ids);
DELETE FROM outbox_events
WHERE aggregate_id IN (SELECT order_id FROM step8_experiment1_order_ids);
DELETE FROM order_status_history
WHERE order_id IN (SELECT order_id FROM step8_experiment1_order_ids);
DELETE FROM order_items
WHERE order_id IN (SELECT order_id FROM step8_experiment1_order_ids);
DELETE FROM orders
WHERE id IN (SELECT order_id FROM step8_experiment1_order_ids);

DROP TEMPORARY TABLE step8_experiment1_order_ids;

ALTER TABLE orders AUTO_INCREMENT = 8200001;
SET SESSION information_schema_stats_expiry = 0;

INSERT INTO merchandises (
    id, name, description, price, sale_price, stock, status, image_url, category,
    created_at, updated_at, deleted_at
) VALUES (
    @product_id, '[SAGA-STEP8-EXP1] Product', '20,000 order partial-success experiment',
    10000, 9000, 25000, 'SALE', NULL, 'CLOTHES', NOW(6), NOW(6), NULL
)
ON DUPLICATE KEY UPDATE
    stock = VALUES(stock), status = 'SALE', deleted_at = NULL, updated_at = NOW(6);

SELECT @experiment_email AS experiment_email,
       @product_id AS product_id,
       @reset_order_count AS reset_order_count,
       @order_id_start AS order_id_start,
       @order_id_start + @expected_order_count - 1 AS order_id_end,
       (SELECT AUTO_INCREMENT
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders') AS actual_next_order_id,
       @expected_order_count AS expected_order_count;
