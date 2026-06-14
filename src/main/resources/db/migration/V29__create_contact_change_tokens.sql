CREATE TABLE contact_change_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    contact_type VARCHAR(16) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    code_salt VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_contact_change_contact_type CHECK (contact_type IN ('EMAIL', 'PHONE')),
    CONSTRAINT chk_contact_change_attempts_non_negative CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX uq_contact_change_code_hash ON contact_change_tokens(code_hash);
CREATE INDEX idx_contact_change_user_contact_active
    ON contact_change_tokens(user_id, contact_type, contact_value, used_at, expires_at);
CREATE INDEX idx_contact_change_created
    ON contact_change_tokens(user_id, contact_type, contact_value, created_at);
