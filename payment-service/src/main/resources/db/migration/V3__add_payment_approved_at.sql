ALTER TABLE payments ADD COLUMN approved_at DATETIME(6) NULL AFTER approved_amount;

UPDATE payments
SET approved_at = updated_at
WHERE status = 'APPROVED'
  AND approved_at IS NULL;
