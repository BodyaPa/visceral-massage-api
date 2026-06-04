DELETE FROM bookings booking
USING specialist_availability_blocks block
WHERE booking.availability_block_id = block.id
  AND booking.status = 'CANCELLED'
  AND block.notes LIKE '%[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]%';

WITH seed(block_marker, client_email, service_title, booking_status, reminder_opt_in, created_offset) AS (
    VALUES
        ('[DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]', 'client.one@dev.ataraksia.local', 'Вісцеральний масаж', 'CONFIRMED', TRUE, INTERVAL '-5 days'),
        ('[DEV_SEED:SPECIALIST_BOOKED_PENDING]', 'client.two@dev.ataraksia.local', 'Консультація', 'AWAITING_PAYMENT_CONFIRMATION', FALSE, INTERVAL '-1 day'),
        ('[DEV_SEED:OWNER_BOOKED_CONFIRMED]', 'client.three@dev.ataraksia.local', 'Комплексний терапевтичний сеанс', 'CONFIRMED', TRUE, INTERVAL '-3 days'),
        ('[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]', 'client.four@dev.ataraksia.local', 'Вісцеральний масаж', 'CANCELLED', FALSE, INTERVAL '-4 days')
)
INSERT INTO bookings (
    user_id, specialist_user_id, service_id, office_id, availability_block_id,
    status, starts_at, ends_at, booked_price, reminder_opt_in, created_at, updated_at
)
SELECT
    client.id,
    block.specialist_user_id,
    service.id,
    block.office_id,
    block.id,
    seed.booking_status,
    block.starts_at,
    block.ends_at,
    service.base_price,
    seed.reminder_opt_in,
    NOW() + seed.created_offset,
    NOW()
FROM seed
JOIN specialist_availability_blocks block ON block.notes LIKE '%' || seed.block_marker || '%'
JOIN users client ON client.email = seed.client_email
JOIN services service ON service.title_ua = seed.service_title
ON CONFLICT (availability_block_id) WHERE status <> 'CANCELLED' DO UPDATE SET
    user_id = EXCLUDED.user_id,
    specialist_user_id = EXCLUDED.specialist_user_id,
    service_id = EXCLUDED.service_id,
    office_id = EXCLUDED.office_id,
    status = EXCLUDED.status,
    starts_at = EXCLUDED.starts_at,
    ends_at = EXCLUDED.ends_at,
    booked_price = EXCLUDED.booked_price,
    reminder_opt_in = EXCLUDED.reminder_opt_in,
    updated_at = NOW();
