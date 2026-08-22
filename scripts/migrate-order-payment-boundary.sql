-- Run only after the Payment service is deployed and legacy payment-key retention is no longer required.
-- Order keeps its business status; Payment owns approval keys from this point onward.
ALTER TABLE orders DROP COLUMN approved_payment_key;
