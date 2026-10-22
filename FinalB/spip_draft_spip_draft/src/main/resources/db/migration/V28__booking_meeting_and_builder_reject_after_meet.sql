ALTER TABLE meeting_requests
ADD COLUMN booking_id BIGINT NULL;

ALTER TABLE meeting_requests
ADD COLUMN from_booking BIT(1) NOT NULL DEFAULT b'0';

ALTER TABLE meeting_requests
ADD CONSTRAINT fk_meeting_requests_booking
FOREIGN KEY (booking_id) REFERENCES bookings(id);