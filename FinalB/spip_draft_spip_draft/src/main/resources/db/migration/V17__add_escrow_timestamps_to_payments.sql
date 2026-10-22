ALTER TABLE payments
    ADD COLUMN escrow_held_at DATETIME NULL,
    ADD COLUMN escrow_released_at DATETIME NULL;