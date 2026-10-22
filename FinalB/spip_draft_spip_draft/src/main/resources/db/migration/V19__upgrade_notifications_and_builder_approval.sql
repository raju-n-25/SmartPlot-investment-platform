-- =========================================
-- BUILDER APPROVAL + NOTIFICATION UPGRADE
-- SAFE / IDEMPOTENT
-- =========================================

-- -----------------------------------------
-- BOOKINGS TABLE
-- -----------------------------------------
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'bookings'
              AND column_name = 'builder_response_at'
        ),
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN builder_response_at DATETIME(6) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'bookings'
              AND column_name = 'builder_response_note'
        ),
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN builder_response_note VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'bookings'
              AND column_name = 'builder_approved_by'
        ),
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN builder_approved_by BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------
-- NOTIFICATIONS TABLE
-- -----------------------------------------
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'type'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT ''GENERAL'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'reference_type'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD COLUMN reference_type VARCHAR(50) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'reference_id'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD COLUMN reference_id BIGINT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND index_name = 'idx_notification_user'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD INDEX idx_notification_user (user_id)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND index_name = 'idx_notification_read'
        ),
        'SELECT 1',
        'ALTER TABLE notifications ADD INDEX idx_notification_read (is_read)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;