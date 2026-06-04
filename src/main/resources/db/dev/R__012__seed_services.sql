WITH seed(title_ua, description_ua, title_en, description_en, duration_minutes, base_price, active, external_payment_url) AS (
    VALUES
        ('Вісцеральний масаж', 'Індивідуальний сеанс м''якої роботи з тілом.', 'Visceral massage', 'An individual gentle bodywork session.', 60, 1500.00, TRUE, 'https://example.com/dev-pay/visceral-massage'),
        ('Консультація', 'Первинна консультація та план подальшої роботи.', 'Consultation', 'Initial consultation and a plan for further work.', 45, 800.00, TRUE, 'https://example.com/dev-pay/consultation'),
        ('Комплексний терапевтичний сеанс', 'Розширений комплексний сеанс тривалістю 90 хвилин.', 'Comprehensive therapy session', 'An extended comprehensive 90-minute session.', 90, 2200.00, TRUE, 'https://example.com/dev-pay/comprehensive-session'),
        ('Неактивна тестова послуга', 'Dev-запис для перевірки фільтра неактивних послуг.', 'Inactive test service', 'Dev record used to verify inactive service filters.', 30, 500.00, FALSE, NULL)
)
UPDATE services service
SET description_ua = seed.description_ua,
    title_en = seed.title_en,
    description_en = seed.description_en,
    duration_minutes = seed.duration_minutes,
    base_price = seed.base_price,
    active = seed.active,
    external_payment_url = seed.external_payment_url,
    updated_at = NOW()
FROM seed
WHERE service.title_ua = seed.title_ua;

WITH seed(title_ua, description_ua, title_en, description_en, duration_minutes, base_price, active, external_payment_url) AS (
    VALUES
        ('Вісцеральний масаж', 'Індивідуальний сеанс м''якої роботи з тілом.', 'Visceral massage', 'An individual gentle bodywork session.', 60, 1500.00, TRUE, 'https://example.com/dev-pay/visceral-massage'),
        ('Консультація', 'Первинна консультація та план подальшої роботи.', 'Consultation', 'Initial consultation and a plan for further work.', 45, 800.00, TRUE, 'https://example.com/dev-pay/consultation'),
        ('Комплексний терапевтичний сеанс', 'Розширений комплексний сеанс тривалістю 90 хвилин.', 'Comprehensive therapy session', 'An extended comprehensive 90-minute session.', 90, 2200.00, TRUE, 'https://example.com/dev-pay/comprehensive-session'),
        ('Неактивна тестова послуга', 'Dev-запис для перевірки фільтра неактивних послуг.', 'Inactive test service', 'Dev record used to verify inactive service filters.', 30, 500.00, FALSE, NULL)
)
INSERT INTO services (
    title_ua, description_ua, title_en, description_en, duration_minutes,
    base_price, active, external_payment_url, created_at, updated_at
)
SELECT
    seed.title_ua, seed.description_ua, seed.title_en, seed.description_en, seed.duration_minutes,
    seed.base_price, seed.active, seed.external_payment_url, NOW(), NOW()
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM services service
    WHERE service.title_ua = seed.title_ua
);
