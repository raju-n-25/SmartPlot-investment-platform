ALTER TABLE booking_agreements
ADD COLUMN agreement_type VARCHAR(50) NOT NULL DEFAULT 'FULL_PAYMENT';

CREATE INDEX idx_agreement_type ON booking_agreements (agreement_type);