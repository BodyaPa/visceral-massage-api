ALTER TABLE bookings
    ADD COLUMN booked_price NUMERIC(12, 2);

UPDATE bookings booking
SET booked_price = service.base_price
FROM services service
WHERE booking.service_id = service.id;

ALTER TABLE bookings
    ALTER COLUMN booked_price SET NOT NULL,
    ADD CONSTRAINT chk_bookings_booked_price_non_negative CHECK (booked_price >= 0);
