SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plots'
      AND COLUMN_NAME = 'coordinates'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE plots ADD COLUMN coordinates LONGTEXT NULL COMMENT ''Normalised polygon coordinates JSON (values in [0,1])''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plots'
      AND COLUMN_NAME = 'shape_type'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE plots ADD COLUMN shape_type VARCHAR(20) NULL COMMENT ''Shape type: RECTANGLE or POLYGON''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plots'
      AND COLUMN_NAME = 'road_facing'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE plots ADD COLUMN road_facing BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''True when the plot faces a road on at least one edge''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plots'
      AND INDEX_NAME = 'idx_plots_road_facing'
);
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_plots_road_facing ON plots(road_facing)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plots'
      AND INDEX_NAME = 'idx_plots_shape_type'
);
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX idx_plots_shape_type ON plots(shape_type)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;