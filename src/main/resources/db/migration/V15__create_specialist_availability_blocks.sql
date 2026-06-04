CREATE TABLE specialist_availability_blocks (
    id BIGSERIAL PRIMARY KEY,
    specialist_user_id BIGINT NOT NULL REFERENCES users(id),
    office_id BIGINT REFERENCES offices(id),
    status VARCHAR(24) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_specialist_availability_status CHECK (status IN ('AVAILABLE', 'BLOCKED')),
    CONSTRAINT chk_specialist_availability_time_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_specialist_availability_user_range
    ON specialist_availability_blocks(specialist_user_id, starts_at, ends_at);

CREATE INDEX idx_specialist_availability_office
    ON specialist_availability_blocks(office_id);
