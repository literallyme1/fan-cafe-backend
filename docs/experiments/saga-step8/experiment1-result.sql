SET @experiment_email = 'saga-step8@fan-cafe.test';

SELECT COUNT(*) AS payment_unknown_count
FROM saga_instance saga
JOIN orders customer_order ON customer_order.id = saga.order_id
JOIN users experiment_user ON experiment_user.id = customer_order.user_id
WHERE experiment_user.email = @experiment_email
  AND payment_unknown_at IS NOT NULL;

SELECT COUNT(*) AS unresolved_saga_count
FROM saga_instance saga
JOIN orders customer_order ON customer_order.id = saga.order_id
JOIN users experiment_user ON experiment_user.id = customer_order.user_id
WHERE experiment_user.email = @experiment_email
  AND payment_unknown_at IS NOT NULL
  AND resolved_at IS NULL;

SELECT saga.status, COUNT(*) AS saga_count
FROM saga_instance saga
JOIN orders customer_order ON customer_order.id = saga.order_id
JOIN users experiment_user ON experiment_user.id = customer_order.user_id
WHERE experiment_user.email = @experiment_email
GROUP BY saga.status
ORDER BY saga.status;

SELECT
    COUNT(*) AS payment_unknown_count,
    SUM(resolved_at IS NOT NULL) AS automatically_resolved_count,
    ROUND(100.0 * SUM(resolved_at IS NOT NULL) / NULLIF(COUNT(*), 0), 4)
        AS automatic_convergence_rate_percent
FROM saga_instance saga
JOIN orders customer_order ON customer_order.id = saga.order_id
JOIN users experiment_user ON experiment_user.id = customer_order.user_id
WHERE experiment_user.email = @experiment_email
  AND payment_unknown_at IS NOT NULL;

WITH convergence AS (
    SELECT
        saga.order_id,
        TIMESTAMPDIFF(MICROSECOND, saga.payment_unknown_at, saga.resolved_at) AS convergence_us,
        ROW_NUMBER() OVER (
            ORDER BY TIMESTAMPDIFF(MICROSECOND, saga.payment_unknown_at, saga.resolved_at), saga.order_id
        ) AS rn,
        COUNT(*) OVER () AS total_count
    FROM saga_instance saga
    JOIN orders customer_order ON customer_order.id = saga.order_id
    JOIN users experiment_user ON experiment_user.id = customer_order.user_id
    WHERE experiment_user.email = @experiment_email
      AND saga.payment_unknown_at IS NOT NULL
      AND saga.resolved_at IS NOT NULL
      AND saga.status IN ('COMPLETED', 'CANCELLED', 'COMPENSATED')
)
SELECT
    MAX(total_count) AS resolved_sample_count,
    ROUND(MAX(CASE WHEN rn = CEIL(total_count * 0.50)
        THEN convergence_us END) / 1000.0, 3) AS convergence_p50_ms,
    ROUND(MAX(CASE WHEN rn = CEIL(total_count * 0.95)
        THEN convergence_us END) / 1000.0, 3) AS convergence_p95_ms
FROM convergence;

SELECT
    saga.order_id,
    saga.status,
    saga.payment_unknown_at,
    saga.resolved_at,
    ROUND(TIMESTAMPDIFF(MICROSECOND, saga.payment_unknown_at, saga.resolved_at) / 1000.0, 3)
        AS convergence_ms
FROM saga_instance saga
JOIN orders customer_order ON customer_order.id = saga.order_id
JOIN users experiment_user ON experiment_user.id = customer_order.user_id
WHERE experiment_user.email = @experiment_email
  AND saga.payment_unknown_at IS NOT NULL
ORDER BY saga.order_id;
