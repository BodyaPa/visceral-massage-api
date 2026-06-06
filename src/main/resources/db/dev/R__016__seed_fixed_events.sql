WITH seed(marker, service_title, specialist_email, office_name, day_offset, starts_at, ends_at, capacity, active, note) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', 'Груповий сеанс', 'specialist@dev.ataraksia.local', 'Ataraksia Center', 6, TIME '08:30', TIME '10:00', 5, TRUE, 'Ранковий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_01]'),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', 'Груповий сеанс', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 9, TIME '12:00', TIME '13:30', 1, TRUE, 'Заповнений тестовий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_FULL]'),
        ('[DEV_EXTRA:WORKSHOP_OPEN]', 'Парний навчальний воркшоп', 'specialist.two@dev.ataraksia.local', 'Ataraksia Podil Room', 2, TIME '18:30', TIME '20:30', 6, TRUE, 'Evening partner workshop [DEV_EXTRA:WORKSHOP_OPEN]'),
        ('[DEV_EXTRA:WORKSHOP_NEAR_FULL]', 'Парний навчальний воркшоп', 'specialist.finance@dev.ataraksia.local', 'Ataraksia Center', 7, TIME '11:00', TIME '13:00', 2, TRUE, 'Nearly full workshop [DEV_EXTRA:WORKSHOP_NEAR_FULL]'),
        ('[DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]', 'Груповий сеанс', 'specialist.two@dev.ataraksia.local', 'Ataraksia Lviv Pop-up', 12, TIME '15:00', TIME '16:30', 4, TRUE, 'Event with cancelled enrollment [DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]'),
        ('[DEV_EXTRA:INACTIVE_EVENT]', 'Парний навчальний воркшоп', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 15, TIME '09:00', TIME '11:00', 3, FALSE, 'Inactive event should not appear publicly [DEV_EXTRA:INACTIVE_EVENT]')
)
UPDATE fixed_events event
SET service_id = service.id,
    specialist_user_id = specialist.id,
    office_id = office.id,
    starts_at = (CURRENT_DATE + seed.day_offset + seed.starts_at) AT TIME ZONE 'Europe/Kyiv',
    ends_at = (CURRENT_DATE + seed.day_offset + seed.ends_at) AT TIME ZONE 'Europe/Kyiv',
    capacity = seed.capacity,
    note = seed.note,
    active = seed.active,
    updated_at = NOW()
FROM seed
JOIN services service ON service.title_ua = seed.service_title
JOIN users specialist ON specialist.email = seed.specialist_email
JOIN offices office ON office.name = seed.office_name
WHERE event.note LIKE '%' || seed.marker || '%';

WITH seed(marker, service_title, specialist_email, office_name, day_offset, starts_at, ends_at, capacity, active, note) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', 'Груповий сеанс', 'specialist@dev.ataraksia.local', 'Ataraksia Center', 6, TIME '08:30', TIME '10:00', 5, TRUE, 'Ранковий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_01]'),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', 'Груповий сеанс', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 9, TIME '12:00', TIME '13:30', 1, TRUE, 'Заповнений тестовий груповий сеанс [DEV_SEED:FIXED_EVENT_GROUP_FULL]'),
        ('[DEV_EXTRA:WORKSHOP_OPEN]', 'Парний навчальний воркшоп', 'specialist.two@dev.ataraksia.local', 'Ataraksia Podil Room', 2, TIME '18:30', TIME '20:30', 6, TRUE, 'Evening partner workshop [DEV_EXTRA:WORKSHOP_OPEN]'),
        ('[DEV_EXTRA:WORKSHOP_NEAR_FULL]', 'Парний навчальний воркшоп', 'specialist.finance@dev.ataraksia.local', 'Ataraksia Center', 7, TIME '11:00', TIME '13:00', 2, TRUE, 'Nearly full workshop [DEV_EXTRA:WORKSHOP_NEAR_FULL]'),
        ('[DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]', 'Груповий сеанс', 'specialist.two@dev.ataraksia.local', 'Ataraksia Lviv Pop-up', 12, TIME '15:00', TIME '16:30', 4, TRUE, 'Event with cancelled enrollment [DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]'),
        ('[DEV_EXTRA:INACTIVE_EVENT]', 'Парний навчальний воркшоп', 'owner@dev.ataraksia.local', 'Ataraksia Studio', 15, TIME '09:00', TIME '11:00', 3, FALSE, 'Inactive event should not appear publicly [DEV_EXTRA:INACTIVE_EVENT]')
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
    seed.active,
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

WITH seed(event_marker, client_phone, enrollment_status, reminder_opt_in, reminder_sent) AS (
    VALUES
        ('[DEV_SEED:FIXED_EVENT_GROUP_01]', '+380990000011', 'ACTIVE', TRUE, FALSE),
        ('[DEV_SEED:FIXED_EVENT_GROUP_FULL]', '+380990000012', 'ACTIVE', FALSE, FALSE),
        ('[DEV_EXTRA:WORKSHOP_OPEN]', '+380990000011', 'ACTIVE', TRUE, FALSE),
        ('[DEV_EXTRA:WORKSHOP_OPEN]', '+380990000027', 'ACTIVE', FALSE, FALSE),
        ('[DEV_EXTRA:WORKSHOP_NEAR_FULL]', '+380990000012', 'ACTIVE', TRUE, TRUE),
        ('[DEV_EXTRA:WORKSHOP_NEAR_FULL]', '+380990000028', 'ACTIVE', FALSE, FALSE),
        ('[DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]', '+380990000013', 'CANCELLED', FALSE, FALSE),
        ('[DEV_EXTRA:GROUP_CANCELLED_ENROLLMENT]', '+380990000014', 'ACTIVE', TRUE, FALSE)
)
INSERT INTO fixed_event_enrollments (
    event_id, user_id, status, reminder_opt_in, reminder_sent_at, created_at, updated_at
)
SELECT
    event.id,
    client.id,
    seed.enrollment_status,
    seed.reminder_opt_in,
    CASE WHEN seed.reminder_sent THEN NOW() - INTERVAL '1 hour' ELSE NULL END,
    NOW() - INTERVAL '1 day',
    NOW()
FROM seed
JOIN fixed_events event ON event.note LIKE '%' || seed.event_marker || '%'
JOIN users client ON client.phone = seed.client_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM fixed_event_enrollments enrollment
    WHERE enrollment.event_id = event.id
      AND enrollment.user_id = client.id
      AND enrollment.status = seed.enrollment_status
);
