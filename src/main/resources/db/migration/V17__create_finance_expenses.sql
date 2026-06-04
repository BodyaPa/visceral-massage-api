CREATE TABLE finance_expenses (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(12, 2) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(500) NOT NULL,
    expense_date DATE NOT NULL,
    office_id BIGINT REFERENCES offices(id),
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_finance_expenses_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_finance_expenses_date ON finance_expenses(expense_date);
CREATE INDEX idx_finance_expenses_office ON finance_expenses(office_id);
