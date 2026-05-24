-- =============================================================================
-- Mock PG Webhook / Outbox k6 부하 테스트용 PAYMENT_PENDING 주문 10,000건 시드
-- =============================================================================
--
-- 대상 DB: MySQL (fan_cafe)
-- 실행 예: mysql -u root -p fan_cafe < scripts/seed-payment-pending-orders.sql
--
-- Spring Boot (test profile)
--   load-test.seed.payment-pending-orders.enabled=true
--   → PaymentPendingOrderLoadTestSeedRunner 가 classpath:scripts/seed-payment-pending-orders.sql 실행
--
-- 선행 조건
--   - 반복 실행 시 먼저 scripts/reset-order-outbox-test-data.sql 실행
--   - outbox_events 는 시드하지 않음 (웹훅 PAYMENT_APPROVED 처리 시 ORDER_CREATED 생성)
--
-- k6 예측 가능 값 (JavaScript 예시)
--   const ORDER_ID_START = 900001;
--   const ORDER_COUNT    = 10000;
--   const APPROVAL_AMOUNT = 9000;
--   const orderId = ORDER_ID_START + ((__VU - 1 + __ITER) % ORDER_COUNT);
--   const idempotencyKey = `K6-MOCK-PG-${orderId}`;
--   const rawBody = JSON.stringify({
--     eventType: 'PAYMENT_APPROVED',
--     orderId,
--     approvalAmount: APPROVAL_AMOUNT,
--     idempotencyKey,
--   });
--
-- Mock PG Webhook
--   POST /api/mock-pg/webhook
--   Headers: X-Mock-PG-Timestamp, X-Mock-PG-Signature
--   Signature: HMAC-SHA256( secret, timestamp + "." + rawBody )  (hex, lowercase)
--   Local secret: mock.pg.webhook-secret (application-local.properties)
--
-- =============================================================================

DELIMITER ;;

DROP PROCEDURE IF EXISTS seed_payment_pending_orders;;

CREATE PROCEDURE seed_payment_pending_orders()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_user_id BIGINT;
    DECLARE v_product_id BIGINT;
    DECLARE v_order_id BIGINT;

  -- k6 와 1:1 매핑되는 고정 상수
    DECLARE order_id_start BIGINT DEFAULT 900001;
    DECLARE order_count INT DEFAULT 10000;
    DECLARE unit_price DECIMAL(19, 2) DEFAULT 9000.00;
    DECLARE item_quantity INT DEFAULT 1;

    DECLARE load_test_user_email VARCHAR(255) DEFAULT 'outbox-k6-loadtest@fan-cafe.test';
    DECLARE load_test_product_name VARCHAR(255) DEFAULT '[K6-OUTBOX-LOAD-TEST] Mock PG Product';

  -- 이미 시드된 경우 중복 방지
    IF EXISTS (
        SELECT 1
        FROM orders
        WHERE id BETWEEN order_id_start AND (order_id_start + order_count - 1)
        LIMIT 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Load test orders already exist (900001-910000). Run reset-order-outbox-test-data.sql first.';
    END IF;

  -- (1) 부하 테스트 전용 사용자
    SELECT id INTO v_user_id
    FROM users
    WHERE email = load_test_user_email
    LIMIT 1;

    IF v_user_id IS NULL THEN
        INSERT INTO users (
            email, password, nickname, role,
            introduction,
            password_updated_at_epoch_sec,
            password_set,
            follower_count,
            following_count,
            created_at,
            updated_at
        ) VALUES (
            load_test_user_email,
            'load-test-encoded-password',
            'outbox-k6-loadtest',
            'USER',
            '',
            UNIX_TIMESTAMP(NOW()),
            1,
            0,
            0,
            NOW(),
            NOW()
        );
        SET v_user_id = LAST_INSERT_ID();
    END IF;

  -- (2) 부하 테스트 전용 상품 (주문 항목 FK / 금액 스냅샷용)
    SELECT id INTO v_product_id
    FROM merchandises
    WHERE name = load_test_product_name
      AND deleted_at IS NULL
    LIMIT 1;

    IF v_product_id IS NULL THEN
        INSERT INTO merchandises (
            name, description, price, sale_price, stock,
            status, image_url, category,
            created_at, updated_at
        ) VALUES (
            load_test_product_name,
            'Mock PG Webhook / Outbox k6 load test fixture',
            10000,
            9000,
            200000,
            'SALE',
            NULL,
            'CLOTHES',
            NOW(),
            NOW()
        );
        SET v_product_id = LAST_INSERT_ID();
    END IF;

  -- (3) PAYMENT_PENDING 주문 + order_items 10,000건
    WHILE i <= order_count DO
        SET v_order_id = order_id_start + i - 1;

        INSERT INTO orders (
            id,
            user_id,
            total_price,
            status,
            approved_payment_key,
            refund_idempotency_key,
            created_at,
            updated_at
        ) VALUES (
            v_order_id,
            v_user_id,
            unit_price * item_quantity,
            'PAYMENT_PENDING',
            NULL,
            NULL,
            NOW(),
            NOW()
        );

        INSERT INTO order_items (
            order_id,
            product_id,
            product_name,
            price,
            quantity,
            created_at,
            updated_at
        ) VALUES (
            v_order_id,
            v_product_id,
            load_test_product_name,
            unit_price,
            item_quantity,
            NOW(),
            NOW()
        );

        SET i = i + 1;
    END WHILE;

  -- (4) 이후 앱 주문 생성과 ID 충돌 방지
    SET @next_auto_increment = (
        SELECT GREATEST(IFNULL(MAX(id), 0) + 1, order_id_start + order_count)
        FROM orders
    );
    SET @alter_sql = CONCAT('ALTER TABLE orders AUTO_INCREMENT = ', @next_auto_increment);
    PREPARE alter_stmt FROM @alter_sql;
    EXECUTE alter_stmt;
    DEALLOCATE PREPARE alter_stmt;

    SELECT
        order_id_start AS order_id_start,
        order_id_start + order_count - 1 AS order_id_end,
        order_count AS seeded_order_count,
        unit_price * item_quantity AS approval_amount_per_order,
        v_user_id AS load_test_user_id,
        v_product_id AS load_test_product_id,
        CONCAT('K6-MOCK-PG-', order_id_start) AS sample_idempotency_key;
END;;

DELIMITER ;

CALL seed_payment_pending_orders();;

DROP PROCEDURE IF EXISTS seed_payment_pending_orders;;
