ALTER TABLE orders
    ADD COLUMN order_type VARCHAR(40) NOT NULL DEFAULT 'MERCHANDISE' AFTER status;

CREATE TABLE campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_amount DECIMAL(19, 2) NOT NULL,
    funded_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    reserved_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    starts_at DATETIME(6) NOT NULL,
    deadline_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    succeeded_at DATETIME(6) NULL,
    failed_at DATETIME(6) NULL,
    refunded_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_campaign_target_amount
        CHECK (target_amount >= 1000 AND MOD(target_amount, 1000) = 0),
    CONSTRAINT ck_campaign_amounts
        CHECK (
            funded_amount >= 0
            AND reserved_amount >= 0
            AND funded_amount + reserved_amount <= target_amount
        ),
    INDEX idx_campaign_deadline (status, deadline_at, id)
);

CREATE TABLE contributions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reserved_at DATETIME(6) NOT NULL,
    approved_at DATETIME(6) NULL,
    confirmed_at DATETIME(6) NULL,
    failed_at DATETIME(6) NULL,
    refund_started_at DATETIME(6) NULL,
    refunded_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_contribution_order_id UNIQUE (order_id),
    CONSTRAINT fk_contribution_campaign_id
        FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_contribution_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_contribution_order_id
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT ck_contribution_amount
        CHECK (amount >= 1000 AND MOD(amount, 1000) = 0),
    INDEX idx_contribution_campaign_status (campaign_id, status, id),
    INDEX idx_contribution_reserved_recovery (status, reserved_at, id)
);
