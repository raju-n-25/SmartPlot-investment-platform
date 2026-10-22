ALTER TABLE payments
ADD COLUMN unfreeze_requested BIT(1) NOT NULL DEFAULT b'0',
ADD COLUMN unfreeze_requested_at DATETIME(6) NULL,
ADD COLUMN unfreeze_request_note VARCHAR(500) NULL;