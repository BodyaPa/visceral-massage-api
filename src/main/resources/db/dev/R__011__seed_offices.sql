WITH seed(name, address, active, phone, email, location_details) AS (
    VALUES
        ('Ataraksia Center', 'Kyiv, 10 Testova Street', TRUE, '+380990001001', 'center@dev.ataraksia.local', 'Second floor, room 204. Dev test office.'),
        ('Ataraksia Studio', 'Kyiv, 25 Spokiina Street', TRUE, '+380990001002', 'studio@dev.ataraksia.local', 'Ground floor, entrance from the courtyard. Dev test office.'),
        ('Ataraksia Archive Office', 'Kyiv, 1 Arkhivna Street', FALSE, '+380990001003', 'archive-office@dev.ataraksia.local', 'Inactive dev office used to verify status filters.')
)
UPDATE offices office
SET address = seed.address,
    active = seed.active,
    phone = seed.phone,
    email = seed.email,
    location_details = seed.location_details,
    updated_at = NOW()
FROM seed
WHERE office.name = seed.name;

WITH seed(name, address, active, phone, email, location_details) AS (
    VALUES
        ('Ataraksia Center', 'Kyiv, 10 Testova Street', TRUE, '+380990001001', 'center@dev.ataraksia.local', 'Second floor, room 204. Dev test office.'),
        ('Ataraksia Studio', 'Kyiv, 25 Spokiina Street', TRUE, '+380990001002', 'studio@dev.ataraksia.local', 'Ground floor, entrance from the courtyard. Dev test office.'),
        ('Ataraksia Archive Office', 'Kyiv, 1 Arkhivna Street', FALSE, '+380990001003', 'archive-office@dev.ataraksia.local', 'Inactive dev office used to verify status filters.')
)
INSERT INTO offices (name, address, active, phone, email, location_details, created_at, updated_at)
SELECT seed.name, seed.address, seed.active, seed.phone, seed.email, seed.location_details, NOW(), NOW()
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM offices office
    WHERE office.name = seed.name
);
