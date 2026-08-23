DELETE FROM payments WHERE order_id BETWEEN 8000001 AND 8020000;

SELECT 8000001 AS order_id_start,
       8020000 AS order_id_end,
       ROW_COUNT() AS deleted_payment_count;
