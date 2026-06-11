CREATE TABLE specialist_finance_settings (
    specialist_user_id BIGINT PRIMARY KEY REFERENCES users(id),
    specialist_share_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    updated_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_specialist_finance_share_percent
        CHECK (specialist_share_percent >= 0 AND specialist_share_percent <= 100)
);
