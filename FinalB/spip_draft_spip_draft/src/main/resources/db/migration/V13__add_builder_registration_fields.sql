-- =========================================
-- V13 — Add builder business fields safely
-- (NO breaking existing data)
-- =========================================

-- Step 1: Add columns as NULL (safe for existing rows)

ALTER TABLE builders
ADD COLUMN company_registration VARCHAR(100) NULL,
ADD COLUMN pan_number VARCHAR(20) NULL,
ADD COLUMN gst_number VARCHAR(30) NULL;

