CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    expected_amount DECIMAL(19, 2) NOT NULL,
    approved_amount DECIMAL(19, 2) NULL,
    payment_key VARCHAR(100) NULL,
    failure_reason VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_order_id UNIQUE (order_id),
    CONSTRAINT uk_payments_payment_key UNIQUE (payment_key)
);
