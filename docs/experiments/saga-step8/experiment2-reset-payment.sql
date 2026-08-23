DELETE FROM payments WHERE order_id BETWEEN 8100001 AND 8104000;

SELECT 8100001 AS order_id_start,
       8104000 AS order_id_end,
       ROW_COUNT() AS deleted_payment_count;
