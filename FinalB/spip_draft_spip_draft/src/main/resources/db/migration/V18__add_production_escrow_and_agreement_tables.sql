-- =========================================
-- PRODUCTION ESCROW + AGREEMENT UPGRADE
-- SAFE / IDEMPOTENT VERSION
-- =========================================

-- -----------------------------------------
-- 1) PAYMENTS TABLE - ADD ONLY MISSING COLUMNS
-- -----------------------------------------

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'escrow_status'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN escrow_status VARCHAR(50) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'escrow_held_at'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN escrow_held_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'hold_until'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN hold_until DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'ready_for_release_at'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN ready_for_release_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'release_scheduled_at'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN release_scheduled_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'released_at'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN released_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'release_reference'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN release_reference VARCHAR(150) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'risk_score'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN risk_score INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'risk_level'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN risk_level VARCHAR(20) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'review_reason'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN review_reason VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'agreement_status'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN agreement_status VARCHAR(50) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'dispute_open'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN dispute_open BIT(1) NOT NULL DEFAULT b''0'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND column_name = 'payout_blocked'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD COLUMN payout_blocked BIT(1) NOT NULL DEFAULT b''0'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------
-- 2) PAYMENTS INDEXES - ADD ONLY IF MISSING
-- -----------------------------------------

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND index_name = 'idx_payment_escrow_status'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD INDEX idx_payment_escrow_status (escrow_status)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'payments'
              AND index_name = 'idx_payment_hold_until'
        ),
        'SELECT 1',
        'ALTER TABLE payments ADD INDEX idx_payment_hold_until (hold_until)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------
-- 3) BACKFILL DEFAULT VALUES SAFELY
-- -----------------------------------------

UPDATE payments
SET escrow_status = CASE
    WHEN is_refunded = 1 THEN 'REFUNDED'
    WHEN is_escrow_released = 1 THEN 'RELEASED'
    WHEN payment_status = 'COMPLETED' THEN 'HELD'
    ELSE escrow_status
END
WHERE escrow_status IS NULL;

UPDATE payments
SET agreement_status = 'NOT_REQUIRED'
WHERE agreement_status IS NULL;

UPDATE payments
SET risk_level = 'LOW'
WHERE risk_level IS NULL;

UPDATE payments
SET risk_score = 0
WHERE risk_score IS NULL;

-- -----------------------------------------
-- 4) AGREEMENT TABLE
-- -----------------------------------------

CREATE TABLE IF NOT EXISTS booking_agreements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    booking_id BIGINT NOT NULL,
    payment_id BIGINT DEFAULT NULL,
    tenant_id BIGINT NOT NULL,
    builder_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,

    agreement_number VARCHAR(100) NOT NULL,
    file_url VARCHAR(1000) DEFAULT NULL,
    status VARCHAR(50) NOT NULL,
    generated_at DATETIME(6) DEFAULT NULL,
    sent_at DATETIME(6) DEFAULT NULL,
    investor_accepted_at DATETIME(6) DEFAULT NULL,
    builder_accepted_at DATETIME(6) DEFAULT NULL,
    signed_at DATETIME(6) DEFAULT NULL,
    expires_at DATETIME(6) DEFAULT NULL,
    remarks VARCHAR(500) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_agreement_booking (booking_id),
    UNIQUE KEY uk_agreement_number (agreement_number),
    KEY idx_agreement_payment (payment_id),
    KEY idx_agreement_status (status),
    KEY idx_agreement_tenant (tenant_id),

    CONSTRAINT fk_agreement_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_agreement_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- -----------------------------------------
-- 5) PAYMENT AUDIT LOG TABLE
-- -----------------------------------------

CREATE TABLE IF NOT EXISTS payment_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    payment_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    old_value VARCHAR(255) DEFAULT NULL,
    new_value VARCHAR(255) DEFAULT NULL,
    actor_type VARCHAR(50) DEFAULT NULL,
    actor_id BIGINT DEFAULT NULL,
    remarks VARCHAR(1000) DEFAULT NULL,

    PRIMARY KEY (id),
    KEY idx_audit_payment (payment_id),
    KEY idx_audit_booking (booking_id),
    KEY idx_audit_tenant (tenant_id),
    KEY idx_audit_event (event_type)
);