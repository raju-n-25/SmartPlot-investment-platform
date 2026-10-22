-- ============================================
-- Align DB with profile photo + user_documents
-- Safe for DBs where Hibernate already created them
-- ============================================

-- 1. add profile_photo_url to users only if missing
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'profile_photo_url'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN profile_photo_url VARCHAR(255) NULL',
    'SELECT "profile_photo_url already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. create user_documents table only if missing
CREATE TABLE IF NOT EXISTS user_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,

    is_current BIT NOT NULL,
    document_type ENUM(
        'PAN_CARD',
        'GST_CERTIFICATE',
        'COMPANY_REGISTRATION',
        'LAND_OWNERSHIP_PROOF',
        'RERA_CERTIFICATE',
        'DC_CONVERSION_DOCUMENT',
        'TAX_DOCUMENT',
        'OTHER_LEGAL_DOCUMENT',
        'BANK_PROOF',
        'ID_PROOF',
        'ADDRESS_PROOF',
        'INCOME_PROOF',
        'OTHER_SUPPORTING_DOCUMENT'
    ) NOT NULL,

    file_url VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NULL,
    rejection_reason TEXT NULL,
    resubmission_reason TEXT NULL,
    reviewed_at DATETIME(6) NULL,
    role VARCHAR(30) NOT NULL,
    status ENUM(
        'NOT_UPLOADED',
        'PARTIAL',
        'COMPLETE',
        'PENDING',
        'APPROVED',
        'REJECTED'
    ) NOT NULL,
    submitted_at DATETIME(6) NULL,
    version_number INT NOT NULL,
    user_id BIGINT NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_user_documents_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- 3. indexes only if missing
SET @idx1_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_documents'
      AND index_name = 'idx_user_documents_user_id'
);

SET @sql = IF(
    @idx1_exists = 0,
    'CREATE INDEX idx_user_documents_user_id ON user_documents(user_id)',
    'SELECT "idx_user_documents_user_id already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx2_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_documents'
      AND index_name = 'idx_user_documents_role_status'
);

SET @sql = IF(
    @idx2_exists = 0,
    'CREATE INDEX idx_user_documents_role_status ON user_documents(role, status)',
    'SELECT "idx_user_documents_role_status already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx3_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_documents'
      AND index_name = 'idx_user_documents_user_type_current'
);

SET @sql = IF(
    @idx3_exists = 0,
    'CREATE INDEX idx_user_documents_user_type_current ON user_documents(user_id, document_type, is_current)',
    'SELECT "idx_user_documents_user_type_current already exists"'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;