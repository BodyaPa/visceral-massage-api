CREATE TABLE membership_offers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    kind VARCHAR(32) NOT NULL,
    title_ua VARCHAR(160) NOT NULL,
    title_en VARCHAR(160) NOT NULL,
    description_ua TEXT,
    description_en TEXT,
    price NUMERIC(12, 2) NOT NULL,
    visits_total INTEGER,
    validity_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE membership_purchases (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL REFERENCES membership_offers(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(48) NOT NULL,
    price_snapshot NUMERIC(12, 2) NOT NULL,
    visits_total INTEGER,
    visits_remaining INTEGER,
    activated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    confirmed_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_membership_purchases_user ON membership_purchases(user_id, created_at DESC);
CREATE INDEX idx_membership_purchases_status ON membership_purchases(status, created_at DESC);

INSERT INTO membership_offers (
    code, kind, title_ua, title_en, description_ua, description_en,
    price, visits_total, validity_days, sort_order
) VALUES
    (
        'care-4',
        'MEMBERSHIP',
        'Абонемент турботи 4',
        'Care membership 4',
        'Чотири індивідуальні сеанси для регулярної підтримки.',
        'Four individual sessions for regular support.',
        7200.00,
        4,
        60,
        10
    ),
    (
        'recovery-8',
        'MEMBERSHIP',
        'Відновлення 8',
        'Recovery 8',
        'Вісім індивідуальних сеансів для довшої програми відновлення.',
        'Eight individual sessions for a longer recovery program.',
        13600.00,
        8,
        120,
        20
    ),
    (
        'gift',
        'CERTIFICATE',
        'Подарунковий сертифікат',
        'Gift certificate',
        'Сертифікат з ручним оформленням і підтвердженням оплати.',
        'Certificate with manual setup and payment confirmation.',
        2500.00,
        NULL,
        90,
        30
    );
