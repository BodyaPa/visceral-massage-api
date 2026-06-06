WITH seed(category, description, amount, expense_date, office_name, finance_email) AS (
    VALUES
        ('Оренда', 'Оренда кабінету [DEV_SEED:EXPENSE_RENT]', 12000.00, CURRENT_DATE - 5, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Матеріали', 'Матеріали для сеансів [DEV_SEED:EXPENSE_MATERIALS]', 1850.00, CURRENT_DATE - 3, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Реклама', 'Тестова рекламна кампанія [DEV_SEED:EXPENSE_ADS]', 3200.00, CURRENT_DATE - 1, 'Ataraksia Studio', 'finance@dev.ataraksia.local'),
        ('Комунальні послуги', 'Електрика та вода [DEV_EXTRA:EXPENSE_UTILITIES]', 1450.00, CURRENT_DATE - 10, 'Ataraksia Podil Room', 'finance@dev.ataraksia.local'),
        ('Обладнання', 'Масажний стіл для pop-up локації [DEV_EXTRA:EXPENSE_EQUIPMENT]', 9800.00, CURRENT_DATE - 8, 'Ataraksia Lviv Pop-up', 'specialist.finance@dev.ataraksia.local'),
        ('Повернення', 'Тестове ручне коригування витрат [DEV_EXTRA:EXPENSE_ADJUSTMENT]', 300.00, CURRENT_DATE - 2, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Навчання', 'Професійний воркшоп команди [DEV_EXTRA:EXPENSE_TRAINING]', 5200.00, CURRENT_DATE + 1, 'Ataraksia Studio', 'specialist.finance@dev.ataraksia.local')
)
UPDATE finance_expenses expense
SET category = seed.category,
    amount = seed.amount,
    expense_date = seed.expense_date,
    office_id = office.id,
    created_by_user_id = finance_user.id,
    updated_at = NOW()
FROM seed
JOIN offices office ON office.name = seed.office_name
JOIN users finance_user ON finance_user.email = seed.finance_email
WHERE expense.description = seed.description;

WITH seed(category, description, amount, expense_date, office_name, finance_email) AS (
    VALUES
        ('Оренда', 'Оренда кабінету [DEV_SEED:EXPENSE_RENT]', 12000.00, CURRENT_DATE - 5, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Матеріали', 'Матеріали для сеансів [DEV_SEED:EXPENSE_MATERIALS]', 1850.00, CURRENT_DATE - 3, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Реклама', 'Тестова рекламна кампанія [DEV_SEED:EXPENSE_ADS]', 3200.00, CURRENT_DATE - 1, 'Ataraksia Studio', 'finance@dev.ataraksia.local'),
        ('Комунальні послуги', 'Електрика та вода [DEV_EXTRA:EXPENSE_UTILITIES]', 1450.00, CURRENT_DATE - 10, 'Ataraksia Podil Room', 'finance@dev.ataraksia.local'),
        ('Обладнання', 'Масажний стіл для pop-up локації [DEV_EXTRA:EXPENSE_EQUIPMENT]', 9800.00, CURRENT_DATE - 8, 'Ataraksia Lviv Pop-up', 'specialist.finance@dev.ataraksia.local'),
        ('Повернення', 'Тестове ручне коригування витрат [DEV_EXTRA:EXPENSE_ADJUSTMENT]', 300.00, CURRENT_DATE - 2, 'Ataraksia Center', 'finance@dev.ataraksia.local'),
        ('Навчання', 'Професійний воркшоп команди [DEV_EXTRA:EXPENSE_TRAINING]', 5200.00, CURRENT_DATE + 1, 'Ataraksia Studio', 'specialist.finance@dev.ataraksia.local')
)
INSERT INTO finance_expenses (
    amount, category, description, expense_date, office_id, created_by_user_id, created_at, updated_at
)
SELECT seed.amount, seed.category, seed.description, seed.expense_date, office.id, finance_user.id, NOW(), NOW()
FROM seed
JOIN offices office ON office.name = seed.office_name
JOIN users finance_user ON finance_user.email = seed.finance_email
WHERE NOT EXISTS (
    SELECT 1
    FROM finance_expenses expense
    WHERE expense.description = seed.description
);

-- Synthetic token rows store hashes only; no plaintext verification or recovery codes are seeded.
WITH seed(user_phone, email, contact_type, contact_value, code_hash, code_salt, attempts, expires_offset, used_offset) AS (
    VALUES
        ('+380990000025', 'phone.recovery@dev.ataraksia.local', 'EMAIL', 'phone.recovery@dev.ataraksia.local', '1000000000000000000000000000000000000000000000000000000000000001', '2000000000000000000000000000000000000000000000000000000000000001', 1, INTERVAL '20 minutes', NULL::INTERVAL),
        ('+380990000026', NULL, 'PHONE', '+380990000026', '1000000000000000000000000000000000000000000000000000000000000002', '2000000000000000000000000000000000000000000000000000000000000002', 2, INTERVAL '10 minutes', NULL::INTERVAL),
        ('+380990000027', 'client.five@dev.ataraksia.local', 'EMAIL', 'client.five@dev.ataraksia.local', '1000000000000000000000000000000000000000000000000000000000000003', '2000000000000000000000000000000000000000000000000000000000000003', 0, INTERVAL '-30 minutes', NULL::INTERVAL),
        ('+380990000028', 'client.six@dev.ataraksia.local', 'EMAIL', 'client.six@dev.ataraksia.local', '1000000000000000000000000000000000000000000000000000000000000004', '2000000000000000000000000000000000000000000000000000000000000004', 3, INTERVAL '-1 day', INTERVAL '-2 hours')
)
INSERT INTO password_recovery_tokens (
    user_id, email, contact_type, contact_value, code_hash, code_salt, attempts, expires_at, used_at, created_at
)
SELECT
    dev_user.id,
    seed.email,
    seed.contact_type,
    seed.contact_value,
    seed.code_hash,
    seed.code_salt,
    seed.attempts,
    NOW() + seed.expires_offset,
    CASE WHEN seed.used_offset IS NULL THEN NULL ELSE NOW() + seed.used_offset END,
    NOW() - INTERVAL '5 minutes'
FROM seed
JOIN users dev_user ON dev_user.phone = seed.user_phone
ON CONFLICT (code_hash) DO NOTHING;

WITH seed(phone, email, first_name, last_name, contact_type, contact_value, code_hash, code_salt, attempts, expires_offset, used_offset) AS (
    VALUES
        ('+380990000031', 'pending.email.registration@dev.ataraksia.local', 'Pending', 'Email Registration', 'EMAIL', 'pending.email.registration@dev.ataraksia.local', '3000000000000000000000000000000000000000000000000000000000000001', '4000000000000000000000000000000000000000000000000000000000000001', 0, INTERVAL '12 minutes', NULL::INTERVAL),
        ('+380990000032', NULL, 'Pending', 'Phone Registration', 'PHONE', '+380990000032', '3000000000000000000000000000000000000000000000000000000000000002', '4000000000000000000000000000000000000000000000000000000000000002', 1, INTERVAL '8 minutes', NULL::INTERVAL),
        ('+380990000033', 'expired.registration@dev.ataraksia.local', 'Expired', 'Registration', 'EMAIL', 'expired.registration@dev.ataraksia.local', '3000000000000000000000000000000000000000000000000000000000000003', '4000000000000000000000000000000000000000000000000000000000000003', 4, INTERVAL '-20 minutes', NULL::INTERVAL),
        ('+380990000034', 'used.registration@dev.ataraksia.local', 'Used', 'Registration', 'EMAIL', 'used.registration@dev.ataraksia.local', '3000000000000000000000000000000000000000000000000000000000000004', '4000000000000000000000000000000000000000000000000000000000000004', 1, INTERVAL '-1 hour', INTERVAL '-30 minutes')
)
INSERT INTO registration_verification_tokens (
    phone, email, first_name, last_name, password_hash, contact_type, contact_value,
    code_hash, code_salt, attempts, expires_at, used_at, created_at
)
SELECT
    seed.phone,
    seed.email,
    seed.first_name,
    seed.last_name,
    '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W',
    seed.contact_type,
    seed.contact_value,
    seed.code_hash,
    seed.code_salt,
    seed.attempts,
    NOW() + seed.expires_offset,
    CASE WHEN seed.used_offset IS NULL THEN NULL ELSE NOW() + seed.used_offset END,
    NOW() - INTERVAL '5 minutes'
FROM seed
ON CONFLICT (code_hash) DO NOTHING;
