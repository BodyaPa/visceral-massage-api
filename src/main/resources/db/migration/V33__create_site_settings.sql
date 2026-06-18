CREATE TABLE site_settings (
    id SMALLINT PRIMARY KEY,
    footer_body_ua TEXT,
    footer_body_en TEXT,
    home_intro_ua TEXT,
    home_intro_en TEXT,
    about_body_ua TEXT,
    about_body_en TEXT,
    contact_body_ua TEXT,
    contact_body_en TEXT,
    updated_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_site_settings_singleton CHECK (id = 1)
);

INSERT INTO site_settings (
    id,
    footer_body_ua,
    footer_body_en,
    home_intro_ua,
    home_intro_en,
    about_body_ua,
    about_body_en,
    contact_body_ua,
    contact_body_en
) VALUES (
    1,
    'Вісцеральний масаж, події, новини та особистий запис у просторі Ataraksia.',
    'Visceral massage, events, news, and personal booking in the Ataraksia space.',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
);
