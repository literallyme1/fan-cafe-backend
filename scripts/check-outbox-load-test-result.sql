-- =============================================================================
-- Mock PG Webhook / Outbox k6 부하 테스트 결과 확인
-- =============================================================================
--
-- 대상 DB: MySQL (fan_cafe)
-- 실행 예: mysql -u root -p fan_cafe < scripts/check-outbox-load-test-result.sql
--
-- 기대 결과 (웹훅 100,000건 모두 성공 처리 후)
--   - orders: PAID 100,000건, PAYMENT_PENDING 0건
--   - outbox_events: ORDER aggregate 100,000건 (status 분포는 Poller/MQ 처리량에 따라 NEW/SENT 혼재)
--   - order_status_history: PAYMENT_PENDING → PAID 100,000건
-- =============================================================================

SET @order_id_start = 900001;
SET @order_id_end = 1000000;
SET @expected_order_count = 100000;
SET @approval_amount = 9000.00;

-- ---------------------------------------------------------------------------
-- 1) 주문 상태 요약
-- ---------------------------------------------------------------------------
SELECT '1) order status summary' AS section;

SELECT
    o.status,
    COUNT(*) AS order_count
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end
GROUP BY o.status
ORDER BY o.status;

SELECT
    COUNT(*) AS total_orders_in_range,
    SUM(CASE WHEN o.status = 'PAYMENT_PENDING' THEN 1 ELSE 0 END) AS payment_pending_count,
    SUM(CASE WHEN o.status = 'PAID' THEN 1 ELSE 0 END) AS paid_count,
    SUM(CASE WHEN o.status = 'PAYMENT_FAILED' THEN 1 ELSE 0 END) AS payment_failed_count,
    SUM(CASE WHEN o.approved_payment_key IS NOT NULL THEN 1 ELSE 0 END) AS orders_with_payment_key,
    ROUND(
        100.0 * SUM(CASE WHEN o.status = 'PAID' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0),
        2
    ) AS paid_ratio_pct
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end;

-- ---------------------------------------------------------------------------
-- 2) Outbox 이벤트 요약 (웹훅 승인 시 ORDER_CREATED payload)
-- ---------------------------------------------------------------------------
SELECT '2) outbox event summary' AS section;

SELECT
    oe.status,
    COUNT(*) AS event_count
FROM outbox_events oe
WHERE oe.aggregate_type = 'ORDER'
  AND oe.aggregate_id BETWEEN @order_id_start AND @order_id_end
GROUP BY oe.status
ORDER BY oe.status;

SELECT
    COUNT(*) AS total_outbox_events,
    SUM(CASE WHEN oe.status = 'NEW' THEN 1 ELSE 0 END) AS new_count,
    SUM(CASE WHEN oe.status = 'SENT' THEN 1 ELSE 0 END) AS sent_count,
    SUM(CASE WHEN oe.status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN oe.status = 'MANUAL_REQUIRED' THEN 1 ELSE 0 END) AS manual_required_count,
    MIN(oe.created_at) AS first_outbox_created_at,
    MAX(oe.created_at) AS last_outbox_created_at
FROM outbox_events oe
WHERE oe.aggregate_type = 'ORDER'
  AND oe.aggregate_id BETWEEN @order_id_start AND @order_id_end;

-- ---------------------------------------------------------------------------
-- 3) 주문 상태 이력 (PAYMENT_PENDING → PAID)
-- ---------------------------------------------------------------------------
SELECT '3) order status history summary' AS section;

SELECT
    h.from_status,
    h.to_status,
    COUNT(*) AS transition_count
FROM order_status_history h
INNER JOIN orders o ON o.id = h.order_id
WHERE o.id BETWEEN @order_id_start AND @order_id_end
GROUP BY h.from_status, h.to_status
ORDER BY h.from_status, h.to_status;

-- ---------------------------------------------------------------------------
-- 4) 데이터 정합성 검증
-- ---------------------------------------------------------------------------
SELECT '4) consistency checks' AS section;

-- PAID 주문인데 Outbox 가 없는 경우
SELECT
    'paid_orders_missing_outbox' AS check_name,
    COUNT(*) AS issue_count
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end
  AND o.status = 'PAID'
  AND NOT EXISTS (
      SELECT 1
      FROM outbox_events oe
      WHERE oe.aggregate_type = 'ORDER'
        AND oe.aggregate_id = o.id
  );

-- Outbox 는 있는데 PAID 가 아닌 경우
SELECT
    'outbox_on_non_paid_orders' AS check_name,
    COUNT(*) AS issue_count
FROM outbox_events oe
INNER JOIN orders o ON o.id = oe.aggregate_id
WHERE oe.aggregate_type = 'ORDER'
  AND oe.aggregate_id BETWEEN @order_id_start AND @order_id_end
  AND o.status <> 'PAID';

-- 승인 금액과 total_price 불일치로 PAYMENT_FAILED 된 주문
SELECT
    'payment_failed_orders' AS check_name,
    COUNT(*) AS issue_count
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end
  AND o.status = 'PAYMENT_FAILED';

-- paymentKey(idempotencyKey) 패턴 불일치
SELECT
    'unexpected_payment_key_pattern' AS check_name,
    COUNT(*) AS issue_count
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end
  AND o.status = 'PAID'
  AND o.approved_payment_key <> CONCAT('K6-MOCK-PG-', o.id);

-- 주문 수가 기대치와 다른 경우
SELECT
    'order_count_mismatch' AS check_name,
    CASE
        WHEN COUNT(*) = @expected_order_count THEN 0
        ELSE 1
    END AS issue_count,
    COUNT(*) AS actual_order_count,
    @expected_order_count AS expected_order_count
FROM orders o
WHERE o.id BETWEEN @order_id_start AND @order_id_end;

-- ---------------------------------------------------------------------------
-- 5) k6 매핑 샘플 (orderId / paymentKey / amount)
-- ---------------------------------------------------------------------------
SELECT '5) k6 mapping sample (first 5 orders)' AS section;

SELECT
    o.id AS order_id,
    o.total_price AS approval_amount,
    CONCAT('K6-MOCK-PG-', o.id) AS idempotency_key,
    o.status,
    o.approved_payment_key AS approved_payment_key
FROM orders o
WHERE o.id BETWEEN @order_id_start AND (@order_id_start + 4)
ORDER BY o.id;

-- ---------------------------------------------------------------------------
-- 6) 최근 Outbox 샘플 payload (ORDER_CREATED)
-- ---------------------------------------------------------------------------
SELECT '6) recent outbox payload sample' AS section;

SELECT
    oe.id AS outbox_id,
    oe.aggregate_id AS order_id,
    oe.status AS outbox_status,
    oe.event_id,
    LEFT(oe.payload, 120) AS payload_preview
FROM outbox_events oe
WHERE oe.aggregate_type = 'ORDER'
  AND oe.aggregate_id BETWEEN @order_id_start AND @order_id_end
ORDER BY oe.id DESC
LIMIT 5;
