CREATE TABLE saga_instance (
    saga_id CHAR(36) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    current_step VARCHAR(40) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (saga_id),
    CONSTRAINT uk_saga_instance_order_id UNIQUE (order_id),
    CONSTRAINT fk_saga_instance_order_id
        FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_saga_recovery (status, next_retry_at)
);
