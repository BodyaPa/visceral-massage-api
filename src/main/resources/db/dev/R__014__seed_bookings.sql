DELETE FROM bookings booking
USING specialist_availability_blocks block
WHERE booking.availability_block_id = block.id
  AND booking.status = 'CANCELLED'
  AND (block.notes LIKE '%[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]%' OR block.notes LIKE '%[DEV_EXTRA:S2_CANCELLED_HISTORY]%');

WITH seed(block_marker, client_phone, service_title, booking_status, reminder_opt_in, reminder_sent, created_offset) AS (
    VALUES
        ('[DEV_SEED:SPECIALIST_BOOKED_CONFIRMED]', '+380990000011', 'Вісцеральний масаж', 'CONFIRMED', TRUE, FALSE, INTERVAL '-5 days'),
        ('[DEV_SEED:SPECIALIST_BOOKED_PENDING]', '+380990000012', 'Консультація', 'AWAITING_PAYMENT_CONFIRMATION', FALSE, FALSE, INTERVAL '-1 day'),
        ('[DEV_SEED:OWNER_BOOKED_CONFIRMED]', '+380990000013', 'Комплексний терапевтичний сеанс', 'CONFIRMED', TRUE, FALSE, INTERVAL '-3 days'),
        ('[DEV_SEED:SPECIALIST_BOOKED_CANCELLED]', '+380990000014', 'Вісцеральний масаж', 'CANCELLED', FALSE, FALSE, INTERVAL '-4 days'),
        ('[DEV_EXTRA:S2_PENDING_EVENING]', '+380990000027', 'Вечірній відновлювальний сеанс', 'AWAITING_PAYMENT_CONFIRMATION', TRUE, FALSE, INTERVAL '-2 hours'),
        ('[DEV_EXTRA:S2_LVIV_CONFIRMED]', '+380990000028', 'Вечірній відновлювальний сеанс', 'CONFIRMED', FALSE, FALSE, INTERVAL '-2 days'),
        ('[DEV_EXTRA:SF_CONFIRMED_REMINDER]', '+380990000025', 'Вісцеральний масаж', 'CONFIRMED', TRUE, TRUE, INTERVAL '-4 days'),
        ('[DEV_EXTRA:SF_FREE_PENDING]', '+380990000026', 'Безкоштовний тестовий слот', 'AWAITING_PAYMENT_CONFIRMATION', FALSE, FALSE, INTERVAL '-30 minutes'),
        ('[DEV_EXTRA:S2_CANCELLED_HISTORY]', '+380990000024', 'Вісцеральний масаж', 'CANCELLED', FALSE, FALSE, INTERVAL '-8 days')
)
INSERT INTO bookings (
    user_id, specialist_user_id, service_id, office_id, availability_block_id,
    status, starts_at, ends_at, booked_price, reminder_opt_in, reminder_sent_at, created_at, updated_at
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
    CASE WHEN seed.reminder_sent THEN NOW() - INTERVAL '1 hour' ELSE NULL END,
    NOW() + seed.created_offset,
    NOW()
FROM seed
JOIN specialist_availability_blocks block ON block.notes LIKE '%' || seed.block_marker || '%'
JOIN users client ON client.phone = seed.client_phone
JOIN services service ON service.title_ua = seed.service_title
WHERE NOT EXISTS (
    SELECT 1
    FROM bookings existing
    WHERE existing.availability_block_id = block.id
      AND existing.user_id = client.id
      AND existing.status = seed.booking_status
);
