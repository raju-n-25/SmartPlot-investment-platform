-- V24__meeting_hold_flow_upgrade.sql

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'from_marketplace'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN from_marketplace BIT(1) NOT NULL DEFAULT b''0'''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'hold_active'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN hold_active BIT(1) NOT NULL DEFAULT b''0'''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'meeting_round'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN meeting_round INT NOT NULL DEFAULT 1'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'released_at'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN released_at DATETIME(6) NULL'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'release_reason'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN release_reason VARCHAR(500) NULL'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'meeting_requests'
        AND COLUMN_NAME = 'bought_from_meeting'
    ),
    'SELECT 1',
    'ALTER TABLE meeting_requests ADD COLUMN bought_from_meeting BIT(1) NOT NULL DEFAULT b''0'''
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;