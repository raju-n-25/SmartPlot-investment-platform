-- ONLY add missing column

ALTER TABLE plots
    ADD COLUMN risk_score INT NOT NULL DEFAULT 0;