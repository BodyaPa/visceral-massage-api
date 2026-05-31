CREATE TABLE roles (
    name VARCHAR(32) PRIMARY KEY
);

INSERT INTO roles (name)
VALUES ('USER'),
       ('MASTER'),
       ('SPECIALIST'),
       ('FINANCE_MANAGER'),
       ('SMM');

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_name VARCHAR(32) NOT NULL REFERENCES roles (name),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_name)
);

CREATE INDEX ix_user_roles_role_name ON user_roles (role_name);

INSERT INTO user_roles (user_id, role_name)
SELECT id, 'USER'
FROM users;

INSERT INTO user_roles (user_id, role_name)
SELECT id, role_name
FROM users
CROSS JOIN (VALUES ('MASTER'), ('SPECIALIST'), ('FINANCE_MANAGER'), ('SMM')) AS owner_roles(role_name)
WHERE role = 'ADMIN';
