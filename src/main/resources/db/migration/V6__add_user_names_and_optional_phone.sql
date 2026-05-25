ALTER TABLE users
    ADD COLUMN first_name VARCHAR(50),
    ADD COLUMN last_name VARCHAR(50),
    ALTER COLUMN phone DROP NOT NULL,
    ADD CONSTRAINT ck_users_contact_present CHECK (phone IS NOT NULL OR email IS NOT NULL);
