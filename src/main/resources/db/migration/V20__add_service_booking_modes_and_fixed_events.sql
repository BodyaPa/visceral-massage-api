ALTER TABLE services
    ADD COLUMN booking_mode VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL_APPOINTMENT',
    ADD CONSTRAINT chk_services_booking_mode CHECK (booking_mode IN ('INDIVIDUAL_APPOINTMENT', 'FIXED_EVENT'));

CREATE TABLE fixed_events (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id),
    specialist_user_id BIGINT NOT NULL REFERENCES users(id),
    office_id BIGINT REFERENCES offices(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    note VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fixed_events_time_range CHECK (ends_at > starts_at),
    CONSTRAINT chk_fixed_events_capacity_positive CHECK (capacity > 0)
);

CREATE INDEX idx_fixed_events_public_range ON fixed_events(active, starts_at, ends_at);
CREATE INDEX idx_fixed_events_office ON fixed_events(office_id);
CREATE INDEX idx_fixed_events_specialist ON fixed_events(specialist_user_id);
CREATE INDEX idx_fixed_events_service ON fixed_events(service_id);

CREATE TABLE fixed_event_enrollments (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES fixed_events(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    reminder_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fixed_event_enrollment_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE UNIQUE INDEX uq_fixed_event_active_enrollment
    ON fixed_event_enrollments(event_id, user_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_fixed_event_enrollments_user ON fixed_event_enrollments(user_id);
