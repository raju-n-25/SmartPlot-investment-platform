-- =========================================
-- 🔥 PLOTS
-- =========================================

ALTER TABLE plots ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE plots ADD COLUMN booking_status VARCHAR(30) DEFAULT 'TOKEN_BLOCKED';
ALTER TABLE plots ADD COLUMN blocked_at DATETIME;

UPDATE plots SET available = TRUE WHERE available IS NULL;

-- =========================================
-- 🔥 BOOKINGS
-- =========================================

ALTER TABLE bookings CHANGE booking_status status VARCHAR(30);

ALTER TABLE bookings ADD COLUMN amount DOUBLE;
ALTER TABLE bookings ADD COLUMN token_expiry DATETIME;
ALTER TABLE bookings ADD COLUMN payment_reference VARCHAR(255);

UPDATE bookings SET status = 'TOKEN_BLOCKED' WHERE status = 'PENDING';

-- =========================================
-- 🔥 PAYMENTS
-- =========================================

ALTER TABLE payments MODIFY payment_status VARCHAR(30);

ALTER TABLE payments ADD COLUMN is_escrow_released BOOLEAN DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN is_refunded BOOLEAN DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN expiry_time DATETIME;
ALTER TABLE payments ADD COLUMN completed_at DATETIME;
ALTER TABLE payments ADD COLUMN refunded_at DATETIME;

UPDATE payments SET payment_status = 'COMPLETED' WHERE payment_status = 'SUCCESS';

-- =========================================
-- 🔥 CONFLICT TABLE
-- =========================================

CREATE TABLE IF NOT EXISTS booking_conflicts (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 plot_id BIGINT,
                                                 investor_id BIGINT,
                                                 reason VARCHAR(255),
                                                 attempt_time DATETIME,
                                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- Fix missing audit columns
ALTER TABLE audit_logs
    ADD COLUMN updated_at DATETIME;
