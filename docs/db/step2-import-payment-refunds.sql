-- 내보낸 CSV를 payment_refund_migration_staging에 적재한 후 Payment DB에서 실행한다.
CREATE TABLE IF NOT EXISTS payment_refund_migration_staging (
    order_id BIGINT NOT NULL PRIMARY KEY,
    refund_idempotency_key VARCHAR(150) NOT NULL,
    refund_reason VARCHAR(500) NULL,
    refunded_at DATETIME(6) NULL
);

UPDATE payments p
JOIN payment_refund_migration_staging s ON s.order_id = p.order_id
SET p.status = 'REFUNDED',
    p.refund_idempotency_key = s.refund_idempotency_key,
    p.refund_reason = s.refund_reason,
    p.refunded_at = s.refunded_at
WHERE p.status IN ('APPROVED', 'REFUNDED');

-- 결과가 0건이어야 Order와 Payment의 환불 상태가 모두 이전된 것이다.
SELECT s.order_id
FROM payment_refund_migration_staging s
LEFT JOIN payments p
    ON p.order_id = s.order_id
   AND p.status = 'REFUNDED'
   AND p.refund_idempotency_key = s.refund_idempotency_key
WHERE p.id IS NULL;

