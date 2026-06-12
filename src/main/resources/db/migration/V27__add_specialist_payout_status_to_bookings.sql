ALTER TABLE bookings
    ADD COLUMN specialist_payout_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN specialist_payout_paid_at TIMESTAMPTZ,
    ADD COLUMN specialist_payout_paid_by_user_id BIGINT REFERENCES users(id);

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_specialist_payout_status
        CHECK (specialist_payout_status IN ('PENDING', 'PAID'));
