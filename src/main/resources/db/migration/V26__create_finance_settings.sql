CREATE TABLE finance_settings (
    id SMALLINT PRIMARY KEY,
    quarterly_tax_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    updated_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_finance_settings_singleton CHECK (id = 1),
    CONSTRAINT chk_finance_settings_quarterly_tax_percent
        CHECK (quarterly_tax_percent >= 0 AND quarterly_tax_percent <= 100)
);

INSERT INTO finance_settings (id, quarterly_tax_percent)
VALUES (1, 0);
