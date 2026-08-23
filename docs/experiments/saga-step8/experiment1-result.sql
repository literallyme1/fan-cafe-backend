SET @order_id_start = 8000001;
SET @order_id_end = 8020000;

SELECT COUNT(*) AS payment_unknown_count
FROM saga_instance
WHERE order_id BETWEEN @order_id_start AND @order_id_end
  AND payment_unknown_at IS NOT NULL;

SELECT COUNT(*) AS unresolved_saga_count
FROM saga_instance
WHERE order_id BETWEEN @order_id_start AND @order_id_end
  AND payment_unknown_at IS NOT NULL
  AND resolved_at IS NULL;

SELECT status, COUNT(*) AS saga_count
FROM saga_instance
WHERE order_id BETWEEN @order_id_start AND @order_id_end
GROUP BY status
ORDER BY status;

SELECT
    COUNT(*) AS payment_unknown_count,
    SUM(resolved_at IS NOT NULL) AS automatically_resolved_count,
    ROUND(100.0 * SUM(resolved_at IS NOT NULL) / NULLIF(COUNT(*), 0), 4)
        AS automatic_convergence_rate_percent
FROM saga_instance
WHERE order_id BETWEEN @order_id_start AND @order_id_end
  AND payment_unknown_at IS NOT NULL;

WITH convergence AS (
    SELECT
        order_id,
        TIMESTAMPDIFF(MICROSECOND, payment_unknown_at, resolved_at) AS convergence_us,
        ROW_NUMBER() OVER (
            ORDER BY TIMESTAMPDIFF(MICROSECOND, payment_unknown_at, resolved_at), order_id
        ) AS rn,
        COUNT(*) OVER () AS total_count
    FROM saga_instance
    WHERE order_id BETWEEN @order_id_start AND @order_id_end
      AND payment_unknown_at IS NOT NULL
      AND resolved_at IS NOT NULL
      AND status IN ('COMPLETED', 'CANCELLED', 'COMPENSATED')
)
SELECT
    MAX(total_count) AS resolved_sample_count,
    ROUND(MAX(CASE WHEN rn = CEIL(total_count * 0.50)
        THEN convergence_us END) / 1000.0, 3) AS convergence_p50_ms,
    ROUND(MAX(CASE WHEN rn = CEIL(total_count * 0.95)
        THEN convergence_us END) / 1000.0, 3) AS convergence_p95_ms
FROM convergence;

SELECT
    order_id,
    status,
    payment_unknown_at,
    resolved_at,
    ROUND(TIMESTAMPDIFF(MICROSECOND, payment_unknown_at, resolved_at) / 1000.0, 3)
        AS convergence_ms
FROM saga_instance
WHERE order_id BETWEEN @order_id_start AND @order_id_end
  AND payment_unknown_at IS NOT NULL
ORDER BY order_id;
