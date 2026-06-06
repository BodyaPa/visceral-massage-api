CREATE TABLE registration_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32),
    email VARCHAR(255),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    contact_type VARCHAR(16) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    code_salt VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_registration_verification_contact_required CHECK (phone IS NOT NULL OR email IS NOT NULL),
    CONSTRAINT chk_registration_verification_contact_type CHECK (contact_type IN ('EMAIL', 'PHONE')),
    CONSTRAINT chk_registration_verification_attempts_non_negative CHECK (attempts >= 0)
);

CREATE UNIQUE INDEX uq_registration_verification_code_hash ON registration_verification_tokens(code_hash);
CREATE INDEX idx_registration_verification_contact_active
    ON registration_verification_tokens(contact_type, contact_value, used_at, expires_at);
CREATE INDEX idx_registration_verification_created
    ON registration_verification_tokens(contact_type, contact_value, created_at);
