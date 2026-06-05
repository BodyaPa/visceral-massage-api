WITH seed(marker, service_title, specialist_email, office_name, day_offset, starts_at, ends_at, capacity, note) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', 'Груповий сеанс', 'specialist@dev.ataraksia.local', 'Ataraksia Center', 6, TIME '08:30', TIME '10:00', 5, 'Ранковий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_01]'),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', 'Груповий сеанс', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 9, TIME '12:00', TIME '13:30', 1, 'Заповнений тестовий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_FULL]')
)
UPDATE fixed_events event
SET service_id = service.id,
    specialist_user_id = specialist.id,
    office_id = office.id,
    starts_at = (CURRENT_DATE + seed.day_offset + seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    ends_at = (CURRENT_DATE + seed.day_offset + seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    capacity = seed.capacity,
    note = seed.note,
    active = TRUE,
    updated_at = NOW()
FROM seed
JOIN services service ON service.title_ua = seed.service_title
JOIN users specialist ON specialist.email = seed.specialist_email
JOIN offices office ON office.name = seed.office_name
WHERE event.note LIKE '%' || seed.marker || '%';

WITH seed(marker, service_title, specialist_email, office_name, day_offset, starts_at, ends_at, capacity, note) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', 'Груповий сеанс', 'specialist@dev.ataraksia.local', 'Ataraksia Center', 6, TIME '08:30', TIME '10:00', 5, 'Ранковий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_01]'),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', 'Груповий сеанс', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 9, TIME '12:00', TIME '13:30', 1, 'Заповнений тестовий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_FULL]')
)
INSERT INTO fixed_events (
    service_id, specialist_user_id, office_id, starts_at, ends_at, capacity, note, active, created_at, updated_at
)
SELECT
    service.id,
    specialist.id,
    office.id,
    (CURRENT_DATE + seed.day_offset + seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    (CURRENT_DATE + seed.day_offset + seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    seed.capacity,
    seed.note,
    TRUE,
    NOW(),
    NOW()
FROM seed
JOIN services service ON service.title_ua = seed.service_title
JOIN users specialist ON specialist.email = seed.specialist_email
JOIN offices office ON office.name = seed.office_name
WHERE NOT EXISTS (
    SELECT 1
    FROM fixed_events event
    WHERE event.note LIKE '%' || seed.marker || '%'
);

WITH seed(event_marker, client_email, reminder_opt_in) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', 'client.one@dev.ataraksia.local', TRUE),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', 'client.two@dev.ataraksia.local', FALSE)
)
INSERT INTO fixed_event_enrollments (
    event_id, user_id, status, reminder_opt_in, created_at, updated_at
)
SELECT
    event.id,
    client.id,
    'ACTIVE',
    seed.reminder_opt_in,
    NOW() - INTERVAL '1 day',
    NOW()
FROM seed
JOIN fixed_events event ON event.note LIKE '%' || seed.event_marker || '%'
JOIN users client ON client.email = seed.client_email
WHERE NOT EXISTS (
    SELECT 1
    FROM fixed_event_enrollments enrollment
    WHERE enrollment.event_id = event.id
      AND enrollment.user_id = client.id
      AND enrollment.status = 'ACTIVE'
);
