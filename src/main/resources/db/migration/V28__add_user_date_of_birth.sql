ALTER TABLE users
    ADD COLUMN date_of_birth DATE;

ALTER TABLE registration_verification_tokens
    ADD COLUMN date_of_birth DATE;
