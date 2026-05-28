-- =============================================================================
-- Mock PG Webhook / Outbox k6 부하 테스트 데이터 초기화
-- =============================================================================
--
-- 대상 DB: MySQL (fan_cafe)
-- 실행 예: mysql -u root -p fan_cafe < scripts/reset-order-outbox-test-data.sql
--
-- 삭제 범위
--   - orders.id 900001 ~ 1000000 (시드 스크립트와 동일 범위, 100,000건)
--   - 연관 order_items, order_status_history, outbox_events
--   - 부하 테스트 전용 user / merchandise (다른 데이터와 겹치지 않는 fixture)
--
-- 참고: 별도 payments 테이블은 없으며, 결제 키는 orders.approved_payment_key 컬럼에 저장됩니다.
--       주문 삭제 시 payment 관련 컬럼도 함께 제거됩니다.
--
-- 수동 초기화용 (Spring 기동 시 seed 프로시저가 자동 reset 포함)
-- Spring test profile + load-test.seed.payment-pending-orders.enabled=true 이면 별도 실행 불필요
--
-- 초기화만 필요할 때: 이 스크립트 단독 실행
-- 초기화 후 재시드: scripts/seed-payment-pending-orders.sql
-- =============================================================================

SET @order_id_start = 900001;
SET @order_id_end = 1000000;
SET @load_test_user_email = 'outbox-k6-loadtest@fan-cafe.test';
SET @load_test_product_name = '[K6-OUTBOX-LOAD-TEST] Mock PG Product';

START TRANSACTION;

-- (1) Outbox — 웹훅 승인 후 생성된 ORDER aggregate 이벤트
DELETE FROM outbox_events
WHERE aggregate_type = 'ORDER'
  AND aggregate_id BETWEEN @order_id_start AND @order_id_end;

-- (2) 주문 상태 이력 — PAYMENT_PENDING → PAID 전이 기록
DELETE FROM order_status_history
WHERE order_id BETWEEN @order_id_start AND @order_id_end;

-- (3) 주문 항목
DELETE FROM order_items
WHERE order_id BETWEEN @order_id_start AND @order_id_end;

-- (4) 주문 (approved_payment_key / refund_idempotency_key 포함)
DELETE FROM orders
WHERE id BETWEEN @order_id_start AND @order_id_end;

-- (5) 부하 테스트 fixture — 해당 사용자의 다른 주문이 없을 때만 삭제
DELETE FROM merchandises
WHERE name = @load_test_product_name
  AND NOT EXISTS (
      SELECT 1
      FROM order_items oi
      WHERE oi.product_id = merchandises.id
  );

DELETE FROM users
WHERE email = @load_test_user_email
  AND NOT EXISTS (
      SELECT 1
      FROM orders o
      WHERE o.user_id = users.id
  );

COMMIT;

-- 초기화 결과 요약
SELECT
    @order_id_start AS order_id_start,
    @order_id_end AS order_id_end,
    (SELECT COUNT(*) FROM orders WHERE id BETWEEN @order_id_start AND @order_id_end) AS remaining_orders,
    (SELECT COUNT(*) FROM outbox_events
     WHERE aggregate_type = 'ORDER'
       AND aggregate_id BETWEEN @order_id_start AND @order_id_end) AS remaining_outbox_events,
    (SELECT COUNT(*) FROM order_status_history
     WHERE order_id BETWEEN @order_id_start AND @order_id_end) AS remaining_status_history,
    (SELECT COUNT(*) FROM users WHERE email = @load_test_user_email) AS remaining_load_test_users,
    (SELECT COUNT(*) FROM merchandises WHERE name = @load_test_product_name) AS remaining_load_test_products;
