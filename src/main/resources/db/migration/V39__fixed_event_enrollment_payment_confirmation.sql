ALTER TABLE fixed_event_enrollments
    ADD COLUMN payment_confirmed_at TIMESTAMPTZ,
    ADD COLUMN payment_confirmed_by_user_id BIGINT REFERENCES users(id);

CREATE INDEX idx_fixed_event_enrollments_payment_confirmed
    ON fixed_event_enrollments(payment_confirmed_at);
