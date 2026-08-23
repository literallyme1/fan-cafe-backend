SET @order_id_start = 8000001;
SET @order_id_end = 8020000;
SET @product_id = 8000001;

DELETE FROM saga_instance WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM outbox_events WHERE aggregate_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM order_status_history WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM order_items WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM orders WHERE id BETWEEN @order_id_start AND @order_id_end;

INSERT INTO merchandises (
    id, name, description, price, sale_price, stock, status, image_url, category,
    created_at, updated_at, deleted_at
) VALUES (
    @product_id, '[SAGA-STEP8-EXP1] Product', '20,000 order partial-success experiment',
    10000, 9000, 25000, 'SALE', NULL, 'CLOTHES', NOW(6), NOW(6), NULL
)
ON DUPLICATE KEY UPDATE
    stock = VALUES(stock), status = 'SALE', deleted_at = NULL, updated_at = NOW(6);

ALTER TABLE orders AUTO_INCREMENT = 8000001;

SELECT @order_id_start AS order_id_start,
       @order_id_end AS order_id_end,
       @product_id AS product_id,
       20000 AS expected_order_count;
