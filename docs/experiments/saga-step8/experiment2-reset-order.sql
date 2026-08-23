SET @order_id_start = 8100001;
SET @order_id_end = 8104000;

DELETE FROM saga_instance WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM outbox_events WHERE aggregate_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM order_status_history WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM order_items WHERE order_id BETWEEN @order_id_start AND @order_id_end;
DELETE FROM orders WHERE id BETWEEN @order_id_start AND @order_id_end;

SELECT @order_id_start AS order_id_start,
       @order_id_end AS order_id_end,
       4000 AS reset_order_count;
