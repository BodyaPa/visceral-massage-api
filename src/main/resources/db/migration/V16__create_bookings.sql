CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    specialist_user_id BIGINT NOT NULL REFERENCES users(id),
    service_id BIGINT NOT NULL REFERENCES services(id),
    office_id BIGINT REFERENCES offices(id),
    availability_block_id BIGINT NOT NULL REFERENCES specialist_availability_blocks(id),
    status VARCHAR(48) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    reminder_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_bookings_status CHECK (status IN ('AWAITING_PAYMENT_CONFIRMATION', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_bookings_time_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_specialist_range ON bookings(specialist_user_id, starts_at, ends_at);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE UNIQUE INDEX uq_bookings_active_availability_block
    ON bookings(availability_block_id)
    WHERE status <> 'CANCELLED';
