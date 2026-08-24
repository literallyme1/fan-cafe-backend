DELETE FROM payments
WHERE payment_key LIKE 'STEP8-PARTIAL-%'
   OR payment_key LIKE 'STEP8-DELAY-PROBE-%';

SELECT ROW_COUNT() AS deleted_payment_count;
