-- ============================================================
-- V27__add_meeting_reschedule_fields.sql
-- Adds reschedule fields safely
-- ============================================================

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'meeting_requests'
              AND COLUMN_NAME = 'requested_reschedule_at'
        ),
        'SELECT 1',
        'ALTER TABLE meeting_requests ADD COLUMN requested_reschedule_at DATETIME NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'meeting_requests'
              AND COLUMN_NAME = 'reschedule_reason'
        ),
        'SELECT 1',
        'ALTER TABLE meeting_requests ADD COLUMN reschedule_reason VARCHAR(1000) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'meeting_requests'
              AND COLUMN_NAME = 'reschedule_requested_at'
        ),
        'SELECT 1',
        'ALTER TABLE meeting_requests ADD COLUMN reschedule_requested_at DATETIME NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;