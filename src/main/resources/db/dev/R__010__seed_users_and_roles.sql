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
    ('+380990000014', 'client.four@dev.ataraksia.local', 'Test', 'Client Four', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000021', 'master.only@dev.ataraksia.local', 'Test', 'Master Only', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000022', 'specialist.two@dev.ataraksia.local', 'Test', 'Specialist Two', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000023', 'specialist.finance@dev.ataraksia.local', 'Test', 'Specialist Finance', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000024', 'disabled.client@dev.ataraksia.local', 'Disabled', 'Client', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', FALSE, NOW(), NOW()),
    ('+380990000025', 'phone.recovery@dev.ataraksia.local', 'Phone', 'Recovery', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000026', NULL, 'Phone Only', 'Client', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000027', 'client.five@dev.ataraksia.local', 'Test', 'Client Five', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW()),
    ('+380990000028', 'client.six@dev.ataraksia.local', 'Test', 'Client Six', '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W', TRUE, NOW(), NOW())
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
    WHERE phone IN (
        '+380990000001',
        '+380990000002',
        '+380990000003',
        '+380990000004',
        '+380990000011',
        '+380990000012',
        '+380990000013',
        '+380990000014',
        '+380990000021',
        '+380990000022',
        '+380990000023',
        '+380990000024',
        '+380990000025',
        '+380990000026',
        '+380990000027',
        '+380990000028'
    )
);

INSERT INTO user_roles (user_id, role_name)
SELECT dev_user.id, assigned_role.role_name
FROM users dev_user
JOIN (
    VALUES
        ('+380990000001', 'USER'),
        ('+380990000001', 'MASTER'),
        ('+380990000001', 'SPECIALIST'),
        ('+380990000001', 'FINANCE_MANAGER'),
        ('+380990000001', 'SMM'),
        ('+380990000002', 'USER'),
        ('+380990000002', 'SPECIALIST'),
        ('+380990000003', 'USER'),
        ('+380990000003', 'FINANCE_MANAGER'),
        ('+380990000004', 'USER'),
        ('+380990000004', 'SMM'),
        ('+380990000011', 'USER'),
        ('+380990000012', 'USER'),
        ('+380990000013', 'USER'),
        ('+380990000014', 'USER'),
        ('+380990000021', 'USER'),
        ('+380990000021', 'MASTER'),
        ('+380990000022', 'USER'),
        ('+380990000022', 'SPECIALIST'),
        ('+380990000023', 'USER'),
        ('+380990000023', 'SPECIALIST'),
        ('+380990000023', 'FINANCE_MANAGER'),
        ('+380990000024', 'USER'),
        ('+380990000025', 'USER'),
        ('+380990000026', 'USER'),
        ('+380990000027', 'USER'),
        ('+380990000028', 'USER')
) AS assigned_role(phone, role_name) ON assigned_role.phone = dev_user.phone
ON CONFLICT (user_id, role_name) DO NOTHING;
