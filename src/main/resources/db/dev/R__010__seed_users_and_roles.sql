-- Dev-only accounts. All accounts use the same valid BCrypt password hash.

INSERT INTO users (phone, email, first_name, last_name, password_hash, enabled, created_at, updated_at)
VALUES
    ('+380990000001', 'owner@dev.ataraksia.local', 'Test', 'Owner', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000002', 'specialist@dev.ataraksia.local', 'Test', 'Specialist', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000003', 'finance@dev.ataraksia.local', 'Test', 'Finance', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000004', 'smm@dev.ataraksia.local', 'Test', 'SMM', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000011', 'client.one@dev.ataraksia.local', 'Test', 'Client One', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000012', 'client.two@dev.ataraksia.local', 'Test', 'Client Two', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000013', 'client.three@dev.ataraksia.local', 'Test', 'Client Three', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000014', 'client.four@dev.ataraksia.local', 'Test', 'Client Four', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW())
ON CONFLICT (phone) DO UPDATE SET
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

DELETE FROM user_roles
WHERE user_id IN (
    SELECT id
    FROM users
    WHERE email IN (
        'owner@dev.ataraksia.local',
        'specialist@dev.ataraksia.local',
        'finance@dev.ataraksia.local',
        'smm@dev.ataraksia.local',
        'client.one@dev.ataraksia.local',
        'client.two@dev.ataraksia.local',
        'client.three@dev.ataraksia.local',
        'client.four@dev.ataraksia.local'
    )
);

INSERT INTO user_roles (user_id, role_name)
SELECT dev_user.id, assigned_role.role_name
FROM users dev_user
JOIN (
    VALUES
        ('owner@dev.ataraksia.local', 'USER'),
        ('owner@dev.ataraksia.local', 'MASTER'),
        ('owner@dev.ataraksia.local', 'SPECIALIST'),
        ('owner@dev.ataraksia.local', 'FINANCE_MANAGER'),
        ('owner@dev.ataraksia.local', 'SMM'),
        ('specialist@dev.ataraksia.local', 'USER'),
        ('specialist@dev.ataraksia.local', 'SPECIALIST'),
        ('finance@dev.ataraksia.local', 'USER'),
        ('finance@dev.ataraksia.local', 'FINANCE_MANAGER'),
        ('smm@dev.ataraksia.local', 'USER'),
        ('smm@dev.ataraksia.local', 'SMM'),
        ('client.one@dev.ataraksia.local', 'USER'),
        ('client.two@dev.ataraksia.local', 'USER'),
        ('client.three@dev.ataraksia.local', 'USER'),
        ('client.four@dev.ataraksia.local', 'USER')
) AS assigned_role(email, role_name) ON assigned_role.email = dev_user.email
ON CONFLICT (user_id, role_name) DO NOTHING;
