WITH seed(category, description, amount, expense_date, office_name) AS (
    VALUES
        ('Оренда', 'Оренда кабінету [DEV_SEED:EXPENSE_RENT]', 12000.00, CURRENT_DATE - 5, 'Ataraksia Center'),
        ('Матеріали', 'Матеріали для сеансів [DEV_SEED:EXPENSE_MATERIALS]', 1850.00, CURRENT_DATE - 3, 'Ataraksia Center'),
        ('Реклама', 'Тестова рекламна кампанія [DEV_SEED:EXPENSE_ADS]', 3200.00, CURRENT_DATE - 1, 'Ataraksia Studio')
)
UPDATE finance_expenses expense
SET category = seed.category,
    amount = seed.amount,
    expense_date = seed.expense_date,
    office_id = office.id,
    updated_at = NOW()
FROM seed
JOIN offices office ON office.name = seed.office_name
WHERE expense.description = seed.description;

WITH seed(category, description, amount, expense_date, office_name) AS (
    VALUES
        ('Оренда', 'Оренда кабінету [DEV_SEED:EXPENSE_RENT]', 12000.00, CURRENT_DATE - 5, 'Ataraksia Center'),
        ('Матеріали', 'Матеріали для сеансів [DEV_SEED:EXPENSE_MATERIALS]', 1850.00, CURRENT_DATE - 3, 'Ataraksia Center'),
        ('Реклама', 'Тестова рекламна кампанія [DEV_SEED:EXPENSE_ADS]', 3200.00, CURRENT_DATE - 1, 'Ataraksia Studio')
)
INSERT INTO finance_expenses (
    amount, category, description, expense_date, office_id, created_by_user_id, created_at, updated_at
)
SELECT seed.amount, seed.category, seed.description, seed.expense_date, office.id, finance_user.id, NOW(), NOW()
FROM seed
JOIN offices office ON office.name = seed.office_name
JOIN users finance_user ON finance_user.email = 'finance@dev.ataraksia.local'
WHERE NOT EXISTS (
    SELECT 1
    FROM finance_expenses expense
    WHERE expense.description = seed.description
);
