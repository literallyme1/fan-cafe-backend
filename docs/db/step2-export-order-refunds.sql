-- Order DB에서 실행하고 결과를 CSV로 내보낸다.
-- Step 2 전환 전에 이미 환불된 주문만 Payment DB로 이전한다.
SELECT
    id AS order_id,
    CONCAT('LEGACY:', id, ':', refund_idempotency_key) AS refund_idempotency_key,
    'migrated from Order DB' AS refund_reason,
    updated_at AS refunded_at
FROM orders
WHERE status = 'REFUNDED'
  AND refund_idempotency_key IS NOT NULL;

