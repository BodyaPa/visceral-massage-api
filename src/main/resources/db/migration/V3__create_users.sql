CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       phone VARCHAR(32) NOT NULL,
                       email VARCHAR(255),
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE UNIQUE INDEX ux_users_phone ON users(phone);
CREATE UNIQUE INDEX ux_users_email_not_null ON users(email) WHERE email IS NOT NULL;

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);