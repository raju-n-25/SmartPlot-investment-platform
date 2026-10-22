-- =========================================
-- 1) BOOKINGS: add missing indexes only
--    plot_id unique issue is already not present
-- =========================================

ALTER TABLE bookings
    ADD INDEX idx_booking_status (status),
    ADD INDEX idx_booking_plot_status (plot_id, status),
    ADD INDEX idx_booking_token_expiry (token_expiry);

-- =========================================
-- 2) IMPROVE booking_conflicts table
-- =========================================

ALTER TABLE booking_conflicts
    ADD COLUMN tenant_id BIGINT NULL,
    ADD COLUMN booking_id BIGINT NULL,
    ADD COLUMN conflict_type VARCHAR(50) NULL;

-- =========================================
-- 3) IMPROVE payments table for cleaner flow
-- =========================================

ALTER TABLE payments
    ADD INDEX idx_payment_booking_status (booking_id, payment_status),
    ADD INDEX idx_payment_expiry (expiry_time);