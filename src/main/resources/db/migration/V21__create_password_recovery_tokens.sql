CREATE TABLE password_recovery_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    code_salt VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_password_recovery_attempts_non_negative CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX uq_password_recovery_code_hash ON password_recovery_tokens(code_hash);
CREATE INDEX idx_password_recovery_email_active ON password_recovery_tokens(email, used_at, expires_at);
CREATE INDEX idx_password_recovery_user ON password_recovery_tokens(user_id);
