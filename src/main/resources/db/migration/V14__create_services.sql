CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    title_ua VARCHAR(160) NOT NULL,
    description_ua TEXT,
    title_en VARCHAR(160),
    description_en TEXT,
    duration_minutes INTEGER NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    external_payment_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_services_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT chk_services_base_price_non_negative CHECK (base_price >= 0)
);

CREATE INDEX idx_services_active ON services(active);
