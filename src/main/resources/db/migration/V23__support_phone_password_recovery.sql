ALTER TABLE password_recovery_tokens
    ALTER COLUMN email DROP NOT NULL,
    ADD COLUMN contact_type VARCHAR(16),
    ADD COLUMN contact_value VARCHAR(255);

UPDATE password_recovery_tokens
SET contact_type = 'EMAIL',
    contact_value = email
WHERE contact_type IS NULL;

ALTER TABLE password_recovery_tokens
    ALTER COLUMN contact_type SET NOT NULL,
    ALTER COLUMN contact_value SET NOT NULL,
    ADD CONSTRAINT chk_password_recovery_contact_type CHECK (contact_type IN ('EMAIL', 'PHONE'));

CREATE INDEX idx_password_recovery_contact_active
    ON password_recovery_tokens(contact_type, contact_value, used_at, expires_at);
