-- Dev-only dense calendar data for manual UI checks. Rebuilt on each repeatable migration run.

DELETE FROM fixed_event_enrollments enrollment
USING fixed_events event
WHERE enrollment.event_id = event.id
  AND event.note LIKE '%[DEV_STRESS:%';

DELETE FROM fixed_events event
WHERE event.note LIKE '%[DEV_STRESS:%';

DELETE FROM bookings booking
USING specialist_availability_blocks block
WHERE booking.availability_block_id = block.id
  AND block.notes LIKE '%[DEV_STRESS:%';

DELETE FROM specialist_availability_blocks block
WHERE block.notes LIKE '%[DEV_STRESS:%';

WITH seed(client_number) AS (
    SELECT generate_series(100, 139)
)
INSERT INTO users (phone, email, first_name, last_name, password_hash, enabled, created_at, updated_at)
SELECT
    '+380990000' || seed.client_number,
    'client.stress.' || seed.client_number || '@dev.ataraksia.local',
    'Stress',
    'Client ' || seed.client_number,
    '$2b$10$DPf.w.4sprEipBmMpwaOUe/zY35IsA0mBEcAU.Wxepie86cphI99W',
    TRUE,
    NOW(),
    NOW()
FROM seed
ON CONFLICT (phone) DO UPDATE SET
    email = EXCLUDED.email,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    password_hash = EXCLUDED.password_hash,
    enabled = EXCLUDED.enabled,
    updated_at = NOW();

WITH seed(client_number) AS (
    SELECT generate_series(100, 139)
)
INSERT INTO user_roles (user_id, role_name)
SELECT dev_user.id, 'USER'
FROM seed
JOIN users dev_user ON dev_user.phone = '+380990000' || seed.client_number
ON CONFLICT (user_id, role_name) DO NOTHING;

WITH specialist_seed(code, specialist_email, office_name) AS (
    VALUES
        ('S1', 'specialist@dev.ataraksia.local', 'Ataraksia Center'),
        ('S2', 'specialist.two@dev.ataraksia.local', 'Ataraksia Podil Room'),
        ('S3', 'specialist.finance@dev.ataraksia.local', 'Ataraksia Studio'),
        ('S4', 'owner@dev.ataraksia.local', 'Ataraksia Lviv Pop-up')
),
day_seed(day_offset) AS (
    SELECT generate_series(1, 21)
),
period_seed(period_code, starts_at, ends_at) AS (
    VALUES
        ('AM', TIME '09:00', TIME '12:00'),
        ('PM', TIME '13:00', TIME '17:00')
)
INSERT INTO specialist_availability_blocks (
    specialist_user_id, office_id, status, item_type, starts_at, ends_at, notes, created_at, updated_at
)
SELECT
    specialist.id,
    office.id,
    'AVAILABLE',
    'OPEN_RANGE',
    (CURRENT_DATE + day_seed.day_offset + period_seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + day_seed.day_offset + period_seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    'Dense dev open range ' || specialist_seed.code || ' day ' || day_seed.day_offset || ' ' || period_seed.period_code ||
        ' [DEV_STRESS:OPEN:' || specialist_seed.code || ':D' || LPAD(day_seed.day_offset::TEXT, 2, '0') || ':' || period_seed.period_code || ']',
    NOW(),
    NOW()
FROM specialist_seed
JOIN users specialist ON specialist.email = specialist_seed.specialist_email
JOIN offices office ON office.name = specialist_seed.office_name
CROSS JOIN day_seed
CROSS JOIN period_seed;

WITH specialist_seed(code, specialist_email, office_name) AS (
    VALUES
        ('S1', 'specialist@dev.ataraksia.local', 'Ataraksia Center'),
        ('S2', 'specialist.two@dev.ataraksia.local', 'Ataraksia Podil Room'),
        ('S3', 'specialist.finance@dev.ataraksia.local', 'Ataraksia Studio'),
        ('S4', 'owner@dev.ataraksia.local', 'Ataraksia Lviv Pop-up')
),
blocked_seed(day_offset, starts_at, ends_at, reason) AS (
    SELECT generated_day.day_offset, TIME '12:00', TIME '13:00', 'Lunch buffer check'
    FROM generate_series(1, 21) AS generated_day(day_offset)
    WHERE generated_day.day_offset % 5 = 0
    UNION ALL
    SELECT generated_day.day_offset, TIME '16:00', TIME '18:00', 'Late blocked admin slot'
    FROM generate_series(1, 21) AS generated_day(day_offset)
    WHERE generated_day.day_offset % 7 = 0
)
INSERT INTO specialist_availability_blocks (
    specialist_user_id, office_id, status, item_type, starts_at, ends_at, notes, created_at, updated_at
)
SELECT
    specialist.id,
    office.id,
    'BLOCKED',
    'BLOCK',
    (CURRENT_DATE + blocked_seed.day_offset + blocked_seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + blocked_seed.day_offset + blocked_seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    blocked_seed.reason || ' ' || specialist_seed.code || ' day ' || blocked_seed.day_offset ||
        ' [DEV_STRESS:BLOCK:' || specialist_seed.code || ':D' || LPAD(blocked_seed.day_offset::TEXT, 2, '0') || ':' ||
        REPLACE(blocked_seed.starts_at::TEXT, ':', '') || ']',
    NOW(),
    NOW()
FROM specialist_seed
JOIN users specialist ON specialist.email = specialist_seed.specialist_email
JOIN offices office ON office.name = specialist_seed.office_name
JOIN blocked_seed ON TRUE;

WITH specialist_seed(code, specialist_email) AS (
    VALUES
        ('S1', 'specialist@dev.ataraksia.local'),
        ('S2', 'specialist.two@dev.ataraksia.local'),
        ('S3', 'specialist.finance@dev.ataraksia.local'),
        ('S4', 'owner@dev.ataraksia.local')
),
day_seed(day_offset) AS (
    SELECT generate_series(1, 18)
),
slot_seed(period_code, starts_at, ends_at, service_title, slot_index) AS (
    VALUES
        ('AM', TIME '09:00', TIME '10:00', 'Вісцеральний масаж', 1),
        ('AM', TIME '10:30', TIME '11:15', 'Консультація', 2),
        ('PM', TIME '13:00', TIME '14:00', 'Вісцеральний масаж', 3),
        ('PM', TIME '15:00', TIME '16:15', 'Вечірній відновлювальний сеанс', 4)
),
booking_seed AS (
    SELECT
        specialist_seed.code,
        specialist_seed.specialist_email,
        day_seed.day_offset,
        slot_seed.period_code,
        slot_seed.starts_at,
        slot_seed.ends_at,
        slot_seed.service_title,
        slot_seed.slot_index,
        100 + ((day_seed.day_offset + slot_seed.slot_index + ASCII(RIGHT(specialist_seed.code, 1))) % 40) AS client_number,
        CASE
            WHEN day_seed.day_offset % 6 = 0 AND slot_seed.slot_index = 2 THEN 'CANCELLED'
            WHEN day_seed.day_offset % 4 = 0 THEN 'AWAITING_PAYMENT_CONFIRMATION'
            ELSE 'CONFIRMED'
        END AS booking_status,
        (day_seed.day_offset + slot_seed.slot_index) % 3 = 0 AS reminder_opt_in,
        (day_seed.day_offset + slot_seed.slot_index) % 8 = 0 AS reminder_sent,
        CASE
            WHEN day_seed.day_offset % 5 = 0
                AND day_seed.day_offset % 4 <> 0
                AND NOT (day_seed.day_offset % 6 = 0 AND slot_seed.slot_index = 2)
                AND slot_seed.slot_index IN (1, 3) THEN 'PAID'
            ELSE 'PENDING'
        END AS payout_status
    FROM specialist_seed
    CROSS JOIN day_seed
    CROSS JOIN slot_seed
)
INSERT INTO bookings (
    user_id, specialist_user_id, service_id, office_id, availability_block_id,
    status, starts_at, ends_at, booked_price, reminder_opt_in, reminder_sent_at,
    specialist_payout_status, specialist_payout_paid_at, specialist_payout_paid_by_user_id,
    created_at, updated_at
)
SELECT
    client.id,
    block.specialist_user_id,
    service.id,
    block.office_id,
    block.id,
    booking_seed.booking_status,
    (CURRENT_DATE + booking_seed.day_offset + booking_seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + booking_seed.day_offset + booking_seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    service.base_price,
    booking_seed.reminder_opt_in,
    CASE WHEN booking_seed.reminder_sent THEN NOW() - INTERVAL '2 hours' ELSE NULL END,
    booking_seed.payout_status,
    CASE WHEN booking_seed.payout_status = 'PAID' THEN NOW() - INTERVAL '1 hour' ELSE NULL END,
    CASE WHEN booking_seed.payout_status = 'PAID' THEN finance_user.id ELSE NULL END,
    NOW() - (booking_seed.day_offset || ' days')::INTERVAL,
    NOW()
FROM booking_seed
JOIN users client ON client.phone = '+380990000' || booking_seed.client_number
JOIN users finance_user ON finance_user.email = 'finance@dev.ataraksia.local'
JOIN services service ON service.title_ua = booking_seed.service_title
JOIN specialist_availability_blocks block
    ON block.notes LIKE '%[DEV_STRESS:OPEN:' || booking_seed.code || ':D' || LPAD(booking_seed.day_offset::TEXT, 2, '0') || ':' || booking_seed.period_code || ']%'
WHERE booking_seed.booking_status <> 'CANCELLED'
   OR booking_seed.day_offset <= 12;

WITH specialist_seed(code, specialist_email, office_name) AS (
    VALUES
        ('S1', 'specialist@dev.ataraksia.local', 'Ataraksia Center'),
        ('S2', 'specialist.two@dev.ataraksia.local', 'Ataraksia Podil Room'),
        ('S3', 'specialist.finance@dev.ataraksia.local', 'Ataraksia Studio'),
        ('S4', 'owner@dev.ataraksia.local', 'Ataraksia Lviv Pop-up')
),
event_seed AS (
    SELECT
        specialist_seed.code,
        specialist_seed.specialist_email,
        specialist_seed.office_name,
        generated_day.day_offset,
        CASE WHEN generated_day.day_offset % 2 = 0 THEN 'Парний навчальний воркшоп' ELSE 'Груповий сеанс' END AS service_title,
        CASE WHEN generated_day.day_offset % 2 = 0 THEN TIME '18:30' ELSE TIME '08:00' END AS starts_at,
        CASE WHEN generated_day.day_offset % 2 = 0 THEN TIME '20:30' ELSE TIME '09:30' END AS ends_at,
        CASE WHEN generated_day.day_offset % 3 = 0 THEN 2 ELSE 6 END AS capacity,
        generated_day.day_offset % 11 <> 0 AS active
    FROM specialist_seed
    CROSS JOIN generate_series(2, 20, 3) AS generated_day(day_offset)
)
INSERT INTO fixed_events (
    service_id, specialist_user_id, office_id, starts_at, ends_at, capacity, note, active, created_at, updated_at
)
SELECT
    service.id,
    specialist.id,
    office.id,
    (CURRENT_DATE + event_seed.day_offset + event_seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + event_seed.day_offset + event_seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    event_seed.capacity,
    'Dense dev fixed event ' || event_seed.code || ' day ' || event_seed.day_offset ||
        ' [DEV_STRESS:EVENT:' || event_seed.code || ':D' || LPAD(event_seed.day_offset::TEXT, 2, '0') || ']',
    event_seed.active,
    NOW(),
    NOW()
FROM event_seed
JOIN services service ON service.title_ua = event_seed.service_title
JOIN users specialist ON specialist.email = event_seed.specialist_email
JOIN offices office ON office.name = event_seed.office_name;

WITH event_seed AS (
    SELECT
        event.id AS event_id,
        event.capacity,
        ROW_NUMBER() OVER (ORDER BY event.id) AS event_index
    FROM fixed_events event
    WHERE event.note LIKE '%[DEV_STRESS:EVENT:%'
      AND event.active = TRUE
),
enrollment_seed AS (
    SELECT
        event_seed.event_id,
        event_seed.event_index,
        generate_series(1, LEAST(event_seed.capacity, CASE WHEN event_seed.event_index % 4 = 0 THEN event_seed.capacity ELSE 3 END)) AS attendee_index
    FROM event_seed
)
INSERT INTO fixed_event_enrollments (
    event_id, user_id, status, reminder_opt_in, reminder_sent_at, created_at, updated_at
)
SELECT
    enrollment_seed.event_id,
    client.id,
    CASE WHEN enrollment_seed.attendee_index = 3 AND enrollment_seed.event_index % 5 = 0 THEN 'CANCELLED' ELSE 'ACTIVE' END,
    enrollment_seed.attendee_index % 2 = 0,
    CASE WHEN enrollment_seed.attendee_index = 2 AND enrollment_seed.event_index % 3 = 0 THEN NOW() - INTERVAL '1 hour' ELSE NULL END,
    NOW() - INTERVAL '2 days',
    NOW()
FROM enrollment_seed
JOIN users client ON client.phone = '+380990000' || (100 + ((enrollment_seed.event_index + enrollment_seed.attendee_index) % 40));
