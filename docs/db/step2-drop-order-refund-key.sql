-- Payment 이관 검증 쿼리가 0건을 반환한 뒤 Order DB에서 실행한다.
ALTER TABLE orders DROP COLUMN refund_idempotency_key;

