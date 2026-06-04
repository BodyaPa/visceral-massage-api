WITH seed(specialist_email, office_name, status, day_offset, starts_at, ends_at, notes, marker) AS (
    VALUES
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 1, TIME '09:00', TIME '10:00', 'Morning visceral massage slot [DEV_SEED:SPECIALIST_AVAILABLE_01]', '[DEV_SEED:SPECIALIST_AVAILABLE_01]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 1, TIME '10:30', TIME '11:30', 'Morning consultation slot [DEV_SEED:SPECIALIST_AVAILABLE_02]', '[DEV_SEED:SPECIALIST_AVAILABLE_02]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 2, TIME '14:00', TIME '15:00', 'Afternoon visceral massage slot [DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]', '[DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 3, TIME '09:00', TIME '10:00', 'Morning visceral massage slot [DEV_SEED:SPECIALIST_BOOKED_PENDING]', '[DEV_SEED:SPECIALIST_BOOKED_PENDING]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 4, TIME '11:00', TIME '12:00', 'Available consultation slot [DEV_SEED:SPECIALIST_AVAILABLE_03]', '[DEV_SEED:SPECIALIST_AVAILABLE_03]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'BLOCKED', 5, TIME '09:00', TIME '18:00', 'Vacation test [DEV_SEED:SPECIALIST_BLOCKED_VACATION]', '[DEV_SEED:SPECIALIST_BLOCKED_VACATION]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'BLOCKED', 7, TIME '13:00', TIME '14:00', 'Dev test block [DEV_SEED:SPECIALIST_BLOCKED_SHORT]', '[DEV_SEED:SPECIALIST_BLOCKED_SHORT]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 2, TIME '09:30', TIME '11:00', 'Studio comprehensive session [DEV_SEED:OWNER_AVAILABLE_01]', '[DEV_SEED:OWNER_AVAILABLE_01]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 4, TIME '15:00', TIME '16:30', 'Studio comprehensive session [DEV_SEED:OWNER_BOOKED_CONFIRMED]', '[DEV_SEED:OWNER_BOOKED_CONFIRMED]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 8, TIME '10:00', TIME '11:00', 'Studio visceral massage slot [DEV_SEED:OWNER_AVAILABLE_02]', '[DEV_SEED:OWNER_AVAILABLE_02]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 14, TIME '16:00', TIME '17:00', 'Studio consultation slot [DEV_SEED:OWNER_AVAILABLE_03]', '[DEV_SEED:OWNER_AVAILABLE_03]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', -2, TIME '12:00', TIME '13:00', 'Cancelled historical slot [DEV_SEED:SPECIALIST_BOOKED_CANCELLED]', '[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]')
)
UPDATE specialist_availability_blocks block
SET specialist_user_id = specialist.id,
    office_id = office.id,
    status = seed.status,
    starts_at = (CURRENT_DATE + seed.day_offset + seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    ends_at = (CURRENT_DATE + seed.day_offset + seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    notes = seed.notes,
    updated_at = NOW()
FROM seed
JOIN users specialist ON specialist.email = seed.specialist_email
JOIN offices office ON office.name = seed.office_name
WHERE block.notes LIKE '%' || seed.marker || '%';

WITH seed(specialist_email, office_name, status, day_offset, starts_at, ends_at, notes, marker) AS (
    VALUES
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 1, TIME '09:00', TIME '10:00', 'Morning visceral massage slot [DEV_SEED:SPECIALIST_AVAILABLE_01]', '[DEV_SEED:SPECIALIST_AVAILABLE_01]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 1, TIME '10:30', TIME '11:30', 'Morning consultation slot [DEV_SEED:SPECIALIST_AVAILABLE_02]', '[DEV_SEED:SPECIALIST_AVAILABLE_02]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 2, TIME '14:00', TIME '15:00', 'Afternoon visceral massage slot [DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]', '[DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 3, TIME '09:00', TIME '10:00', 'Morning visceral massage slot [DEV_SEED:SPECIALIST_BOOKED_PENDING]', '[DEV_SEED:SPECIALIST_BOOKED_PENDING]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', 4, TIME '11:00', TIME '12:00', 'Available consultation slot [DEV_SEED:SPECIALIST_AVAILABLE_03]', '[DEV_SEED:SPECIALIST_AVAILABLE_03]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'BLOCKED', 5, TIME '09:00', TIME '18:00', 'Vacation test [DEV_SEED:SPECIALIST_BLOCKED_VACATION]', '[DEV_SEED:SPECIALIST_BLOCKED_VACATION]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'BLOCKED', 7, TIME '13:00', TIME '14:00', 'Dev test block [DEV_SEED:SPECIALIST_BLOCKED_SHORT]', '[DEV_SEED:SPECIALIST_BLOCKED_SHORT]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 2, TIME '09:30', TIME '11:00', 'Studio comprehensive session [DEV_SEED:OWNER_AVAILABLE_01]', '[DEV_SEED:OWNER_AVAILABLE_01]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 4, TIME '15:00', TIME '16:30', 'Studio comprehensive session [DEV_SEED:OWNER_BOOKED_CONFIRMED]', '[DEV_SEED:OWNER_BOOKED_CONFIRMED]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 8, TIME '10:00', TIME '11:00', 'Studio visceral massage slot [DEV_SEED:OWNER_AVAILABLE_02]', '[DEV_SEED:OWNER_AVAILABLE_02]'),
        ('owner@dev.ataraksia.local', 'Ataraksia Studio', 'AVAILABLE', 14, TIME '16:00', TIME '17:00', 'Studio consultation slot [DEV_SEED:OWNER_AVAILABLE_03]', '[DEV_SEED:OWNER_AVAILABLE_03]'),
        ('specialist@dev.ataraksia.local', 'Ataraksia Center', 'AVAILABLE', -2, TIME '12:00', TIME '13:00', 'Cancelled historical slot [DEV_SEED:SPECIALIST_BOOKED_CANCELLED]', '[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]')
)
INSERT INTO specialist_availability_blocks (
    specialist_user_id, office_id, status, starts_at, ends_at, notes, created_at, updated_at
)
SELECT
    specialist.id,
    office.id,
    seed.status,
    (CURRENT_DATE + seed.day_offset + seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + seed.day_offset + seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    seed.notes,
    NOW(),
    NOW()
FROM seed
JOIN users specialist ON specialist.email = seed.specialist_email
JOIN offices office ON office.name = seed.office_name
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist_availability_blocks block
    WHERE block.notes LIKE '%' || seed.marker || '%'
);

UPDATE bookings booking
SET starts_at = block.starts_at,
    ends_at = block.ends_at,
    specialist_user_id = block.specialist_user_id,
    office_id = block.office_id,
    updated_at = NOW()
FROM specialist_availability_blocks block
WHERE booking.availability_block_id = block.id
  AND block.notes LIKE '%[DEV_SEED:%';
