UPDATE saga_instance
SET next_retry_at = CURRENT_TIMESTAMP(6)
WHERE status = 'PAYMENT_UNKNOWN'
  AND next_retry_at IS NULL;

-- 기존 COMPENSATING도 정상 MQ 결과 대기 시간을 먼저 보장한다.
-- 기본 saga.recovery.refund-result-timeout=1m과 동일한 보정값이다.
UPDATE saga_instance
SET next_retry_at = DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 MINUTE)
WHERE status = 'COMPENSATING'
  AND next_retry_at IS NULL;
