CREATE TABLE specialist_availability_blocks (
    id BIGSERIAL PRIMARY KEY,
    specialist_user_id BIGINT NOT NULL REFERENCES users(id),
    office_id BIGINT REFERENCES offices(id),
    status VARCHAR(24) NOT NULL,
    item_type VARCHAR(32) NOT NULL DEFAULT 'OPEN_RANGE',
    service_id BIGINT REFERENCES services(id),
    capacity INTEGER,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_specialist_availability_status CHECK (status IN ('AVAILABLE', 'BLOCKED')),
    CONSTRAINT chk_specialist_availability_item_type CHECK (item_type IN ('OPEN_RANGE', 'APPOINTMENT_SLOT', 'BLOCK')),
    CONSTRAINT chk_specialist_availability_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_specialist_availability_type_status CHECK (
        (status = 'BLOCKED' AND item_type = 'BLOCK' AND service_id IS NULL AND capacity IS NULL)
        OR (status = 'AVAILABLE' AND item_type IN ('OPEN_RANGE', 'APPOINTMENT_SLOT'))
    ),
    CONSTRAINT chk_specialist_availability_slot_service CHECK (
        (item_type = 'APPOINTMENT_SLOT' AND service_id IS NOT NULL AND capacity IS NOT NULL)
        OR item_type <> 'APPOINTMENT_SLOT'
    ),
    CONSTRAINT chk_specialist_availability_time_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_specialist_availability_user_range
    ON specialist_availability_blocks(specialist_user_id, starts_at, ends_at);

CREATE INDEX idx_specialist_availability_office
    ON specialist_availability_blocks(office_id);

CREATE INDEX idx_specialist_availability_service
    ON specialist_availability_blocks(service_id);
